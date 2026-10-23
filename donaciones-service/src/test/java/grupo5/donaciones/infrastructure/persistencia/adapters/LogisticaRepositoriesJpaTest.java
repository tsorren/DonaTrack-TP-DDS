package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.config.ClockConfig;
import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.infrastructure.idempotency.EventoConsumido;
import grupo5.donaciones.infrastructure.idempotency.IEventosConsumidosRepository;
import grupo5.donaciones.infrastructure.persistencia.mapper.LogisticaPersistenciaMapper;
import grupo5.donaciones.models.entities.logistica.EstadoSolicitudEntrega;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import grupo5.donaciones.services.impl.LogisticaBroker;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.EstadoEntradaOutbox;
import grupo5.donaciones.services.logistica.IEstrategiaSeleccionProveedor;
import grupo5.donaciones.services.logistica.ILogisticaOutbox;
import grupo5.donaciones.services.logistica.ResultadoEnvio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Paso 10: solicitud de entrega, outbox del broker (Transactional Outbox) e idempotencia. */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  LogisticaPersistenciaMapper.class,
  SolicitudEntregaRepositoryJpaAdapter.class,
  LogisticaOutboxJpaAdapter.class,
  EventosConsumidosRepositoryJpaAdapter.class,
  LogisticaBroker.class,
  ClockConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class LogisticaRepositoriesJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private ISolicitudesEntregaRepository solicitudes;
  @Autowired private ILogisticaOutbox outbox;
  @Autowired private IEventosConsumidosRepository eventosConsumidos;
  @Autowired private LogisticaBroker broker;
  @Autowired private JdbcTemplate jdbc;

  @MockitoBean private IEstrategiaSeleccionProveedor estrategia;
  @MockitoSpyBean private LogisticaOutboxJpaAdapter outboxEspiado;

  private UUID donacionId;
  private final UUID beneficiarioId = UUID.randomUUID();

  @BeforeEach
  void preparar() {
    jdbc.update("DELETE FROM donaciones.evento_consumido");
    jdbc.update("DELETE FROM donaciones.outbox_logistica");
    jdbc.update("DELETE FROM donaciones.solicitud_entrega");
    jdbc.update("DELETE FROM donaciones.donacion_independiente");
    jdbc.update("DELETE FROM donaciones.donacion");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.persona");
    donacionId = crearDonacionIndependiente();
    when(estrategia.ordenar(any())).thenReturn(List.of("amqp", "http"));
  }

  // Por SQL: alcanza con las filas que exigen las FK
  private UUID crearDonacionIndependiente() {
    UUID donanteId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'HUMANA')", donanteId);
    jdbc.update(
        "INSERT INTO donaciones.humana (id, nombre, apellido) VALUES (?, 'Ana', 'Prueba')",
        donanteId);
    jdbc.update("INSERT INTO donaciones.donante (id, activo) VALUES (?, true)", donanteId);
    UUID donacion = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.donacion (id, id_donante, descripcion, fecha, estado_donacion)"
            + " VALUES (?, ?, 'Donación de prueba', now(), 'SEGMENTADA')",
        donacion,
        donanteId);
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.donacion_independiente (id, id_donacion, estado_actual,"
            + " fecha_registro) VALUES (?, ?, 'ASIGNACION_REALIZADA', now())",
        id,
        donacion);
    return id;
  }

  private DatosEntregaLogistica datos() {
    return new DatosEntregaLogistica(
        donacionId,
        beneficiarioId,
        new DestinoEventoDTO(
            "Av. Corrientes", 1234, 3, "B", "C1043", "CABA", "Buenos Aires", "Argentina"),
        12.5,
        0.75,
        LocalDateTime.of(2026, 10, 22, 10, 30, 15, 123456789));
  }

  private int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM donaciones." + tabla, Integer.class);
  }

  @Test
  void guardarYLeer_solicitud_conservaDescartadosProveedorEstadoYRonda() {
    SolicitudEntrega solicitud = new SolicitudEntrega(donacionId, LocalDateTime.now());
    solicitud.asignarProveedor("amqp");
    solicitud.descartarProveedorActual();
    solicitud.asignarProveedor("http");

    solicitudes.save(solicitud);
    SolicitudEntrega leida = solicitudes.findById(solicitud.getId()).orElseThrow();

    assertEquals(Set.of("amqp"), leida.getProveedoresDescartados());
    assertEquals("http", leida.getProveedorActual());
    assertEquals(EstadoSolicitudEntrega.PENDIENTE, leida.getEstado());
    assertEquals(1, leida.getRonda());
    assertTrue(leida.fueDescartado("amqp"));
  }

  @Test
  void buscarPorDonacion_distingueLaActivaDeLaMasReciente() {
    SolicitudEntrega fallida = new SolicitudEntrega(donacionId, LocalDateTime.now().minusHours(1));
    fallida.marcarFallida();
    solicitudes.save(fallida);
    assertTrue(solicitudes.findActivaPorDonacion(donacionId).isEmpty());

    SolicitudEntrega nueva = new SolicitudEntrega(donacionId, LocalDateTime.now());
    solicitudes.save(nueva);

    assertEquals(
        nueva.getId(), solicitudes.findActivaPorDonacion(donacionId).orElseThrow().getId());
    assertEquals(
        nueva.getId(), solicitudes.findMasRecientePorDonacion(donacionId).orElseThrow().getId());
  }

  @Test
  void guardar_dosSolicitudesActivasParaLaMismaDonacion_loRechazaLaBase() {
    solicitudes.save(new SolicitudEntrega(donacionId, LocalDateTime.now()));
    SolicitudEntrega segunda = new SolicitudEntrega(donacionId, LocalDateTime.now());

    assertThrows(DataIntegrityViolationException.class, () -> solicitudes.save(segunda));
  }

  @Test
  void guardarYLeer_entradaDelOutbox_conservaElPayloadCompleto() {
    SolicitudEntrega solicitud =
        solicitudes.save(new SolicitudEntrega(donacionId, LocalDateTime.now()));
    EntradaOutboxLogistica entrada =
        EntradaOutboxLogistica.nueva(
            solicitud.getId(), "amqp", datos(), "trace-1", 5, LocalDateTime.now());

    outbox.guardar(entrada);
    EntradaOutboxLogistica leida = outbox.buscarPorId(entrada.getId()).orElseThrow();

    // D6: el pedido vuelve exactamente igual, para reintentar lo que se decidió
    assertEquals(datos(), leida.getDatos());
    assertEquals("amqp", leida.getProveedorId());
    assertEquals("trace-1", leida.getTraceId());
    assertEquals(5, leida.getMaxIntentos());
    assertEquals(
        "object",
        jdbc.queryForObject(
            "SELECT jsonb_typeof(payload) FROM donaciones.outbox_logistica", String.class));
  }

  @Test
  void pendientesListas_devuelveSoloLasPendientesVencidasEnOrden() {
    SolicitudEntrega solicitud =
        solicitudes.save(new SolicitudEntrega(donacionId, LocalDateTime.now()));
    LocalDateTime ahora = LocalDateTime.now();
    EntradaOutboxLogistica vieja =
        EntradaOutboxLogistica.nueva(
            solicitud.getId(), "amqp", datos(), "t", 5, ahora.minusMinutes(5));
    EntradaOutboxLogistica reciente =
        EntradaOutboxLogistica.nueva(
            solicitud.getId(), "http", datos(), "t", 5, ahora.minusMinutes(1));
    EntradaOutboxLogistica futura =
        EntradaOutboxLogistica.nueva(
            solicitud.getId(), "amqp", datos(), "t", 5, ahora.plusMinutes(5));
    EntradaOutboxLogistica publicada =
        EntradaOutboxLogistica.nueva(
            solicitud.getId(), "http", datos(), "t", 5, ahora.minusMinutes(9));
    publicada.marcarPublicada();
    List.of(reciente, futura, publicada, vieja).forEach(outbox::guardar);

    assertEquals(
        List.of(vieja.getId(), reciente.getId()),
        outbox.pendientesListas(ahora).stream().map(EntradaOutboxLogistica::getId).toList());
  }

  @Test
  void guardar_intentoIncierto_actualizaIntentosYProximoIntento() {
    SolicitudEntrega solicitud =
        solicitudes.save(new SolicitudEntrega(donacionId, LocalDateTime.now()));
    EntradaOutboxLogistica entrada =
        EntradaOutboxLogistica.nueva(
            solicitud.getId(), "amqp", datos(), "t", 5, LocalDateTime.now());
    outbox.guardar(entrada);

    EntradaOutboxLogistica leida = outbox.buscarPorId(entrada.getId()).orElseThrow();
    leida.registrarIntentoIncierto(LocalDateTime.now(), 30);
    outbox.guardar(leida);

    EntradaOutboxLogistica releida = outbox.buscarPorId(entrada.getId()).orElseThrow();
    assertEquals(1, releida.getIntentos());
    assertTrue(releida.estaPendiente());
    assertFalse(releida.estaListaPara(LocalDateTime.now()));
  }

  @Test
  void eventoConsumido_seReconoceAunqueNoTengaBusinessIdYNoSeRegistraDosVeces() {
    EventoConsumido evento =
        new EventoConsumido(
            UUID.randomUUID(), "RutaAsignada", "cola", null, donacionId, LocalDateTime.now());

    assertFalse(eventosConsumidos.yaFueConsumido("RutaAsignada", null, donacionId));
    eventosConsumidos.registrar(evento);

    assertTrue(eventosConsumidos.yaFueConsumido("RutaAsignada", null, donacionId));
    assertFalse(eventosConsumidos.yaFueConsumido("RutaIniciada", null, donacionId));
    EventoConsumido duplicado =
        new EventoConsumido(
            UUID.randomUUID(), "RutaAsignada", "cola", null, donacionId, LocalDateTime.now());
    assertThrows(
        DataIntegrityViolationException.class, () -> eventosConsumidos.registrar(duplicado));
  }

  @Test
  void solicitarEntrega_guardaLaSolicitudYSuEntradaJuntas() {
    broker.solicitarEntrega(datos());

    assertEquals(1, contar("solicitud_entrega"));
    assertEquals(1, contar("outbox_logistica"));
  }

  @Test
  void solicitarEntrega_siFallaElOutbox_noQuedaLaSolicitud() {
    doThrow(new IllegalStateException("outbox caído")).when(outboxEspiado).guardar(any());

    assertThrows(IllegalStateException.class, () -> broker.solicitarEntrega(datos()));

    // Transactional Outbox: o se guardan las dos o ninguna
    assertEquals(0, contar("solicitud_entrega"));
    assertEquals(0, contar("outbox_logistica"));
  }

  @Test
  void registrarResultado_rechazosDeTodosLosProveedores_recuerdaLosDescartadosEntreLecturas() {
    broker.solicitarEntrega(datos());
    EntradaOutboxLogistica primera = outbox.pendientesListas(LocalDateTime.now()).getFirst();

    broker.registrarResultado(primera.getId(), ResultadoEnvio.RECHAZADO);
    EntradaOutboxLogistica segunda = outbox.pendientesListas(LocalDateTime.now()).getFirst();
    assertEquals("http", segunda.getProveedorId());
    broker.registrarResultado(segunda.getId(), ResultadoEnvio.RECHAZADO);

    // Sin los descartados guardados volvería a "amqp" en la misma ronda, sin terminar nunca
    SolicitudEntrega solicitud = solicitudes.findActivaPorDonacion(donacionId).orElseThrow();
    assertEquals(2, solicitud.getRonda());
    assertEquals("amqp", solicitud.getProveedorActual());
    assertTrue(solicitud.getProveedoresDescartados().isEmpty());
    assertEquals(
        EstadoEntradaOutbox.FALLIDO, outbox.buscarPorId(segunda.getId()).orElseThrow().getEstado());
  }
}
