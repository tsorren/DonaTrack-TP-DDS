package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ValidationException;
import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.infrastructure.outbox.LogisticaOutboxEnMemoria;
import grupo5.donaciones.models.entities.logistica.EstadoSolicitudEntrega;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.impl.SolicitudesEntregaRepositoryEnMemoria;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.EstadoEntradaOutbox;
import grupo5.donaciones.services.logistica.ICatalogoProveedoresLogistica;
import grupo5.donaciones.services.logistica.ResultadoEnvio;
import grupo5.donaciones.services.logistica.ResultadoPedidoCancelacion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class LogisticaBrokerTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, Month.OCTOBER, 7, 12, 0);
  private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

  private SolicitudesEntregaRepositoryEnMemoria solicitudes;
  private LogisticaOutboxEnMemoria outbox;
  private LogisticaBroker broker;
  private ICatalogoProveedoresLogistica catalogo;

  @BeforeEach
  void setUp() {
    catalogo = mock(ICatalogoProveedoresLogistica.class);
    when(catalogo.admiteCancelacion("donatrack")).thenReturn(true);
    solicitudes = new SolicitudesEntregaRepositoryEnMemoria();
    outbox = new LogisticaOutboxEnMemoria();
    broker = brokerCon(5);
  }

  @AfterEach
  void limpiarMdc() {
    MDC.clear();
  }

  private LogisticaBroker brokerCon(int maxIntentos) {
    return new LogisticaBroker(
        solicitudes,
        outbox,
        new SeleccionPorPreferenciaConFallback(List.of("donatrack", "externo"), "donatrack"),
        catalogo,
        CLOCK,
        maxIntentos,
        30);
  }

  private static DatosEntregaLogistica datos(UUID donacionId) {
    return new DatosEntregaLogistica(donacionId, UUID.randomUUID(), null, 10.0, 1.0, AHORA);
  }

  private SolicitudEntrega unicaSolicitud() {
    List<SolicitudEntrega> todas = solicitudes.findAll();
    assertEquals(1, todas.size());
    return todas.get(0);
  }

  private EntradaOutboxLogistica unicaPendiente() {
    List<EntradaOutboxLogistica> pendientes = outbox.pendientesListas(AHORA.plusYears(1));
    assertEquals(1, pendientes.size());
    return pendientes.get(0);
  }

  @Test
  void solicitarEntrega_deberiaRegistrarLaSolicitudYDejarlaPendienteParaElPreferido() {
    UUID donacionId = UUID.randomUUID();

    broker.solicitarEntrega(datos(donacionId));

    SolicitudEntrega solicitud = unicaSolicitud();
    assertEquals(donacionId, solicitud.getDonacionIndependienteId());
    assertEquals(EstadoSolicitudEntrega.PENDIENTE, solicitud.getEstado());
    assertEquals("donatrack", solicitud.getProveedorActual());

    EntradaOutboxLogistica entrada = unicaPendiente();
    assertEquals("donatrack", entrada.getProveedorId());
    assertEquals(solicitud.getId(), entrada.getSolicitudId());
    assertTrue(entrada.estaListaPara(AHORA));
  }

  @Test
  void solicitarEntrega_deberiaUsarElTraceIdActual() {
    MDC.put(FeignTraceRequestInterceptor.MDC_TRACE_KEY, "trace-123");

    broker.solicitarEntrega(datos(UUID.randomUUID()));

    assertEquals("trace-123", unicaPendiente().getTraceId());
  }

  @Test
  void solicitarEntrega_deberiaIgnorarElPedido_CuandoLaDonacionYaTieneUnaSolicitudActiva() {
    UUID donacionId = UUID.randomUUID();
    broker.solicitarEntrega(datos(donacionId));

    broker.solicitarEntrega(datos(donacionId));

    assertEquals(1, solicitudes.count());
    unicaPendiente();
  }

  @Test
  void solicitarEntrega_deberiaPermitirUnaNuevaSolicitud_CuandoLaAnteriorFallo() {
    UUID donacionId = UUID.randomUUID();
    broker.solicitarEntrega(datos(donacionId));
    broker.registrarResultado(unicaPendiente().getId(), ResultadoEnvio.ERROR_CONTRATO);

    broker.solicitarEntrega(datos(donacionId));

    assertEquals(2, solicitudes.count());
    assertTrue(solicitudes.findActivaPorDonacion(donacionId).isPresent());
  }

  @Test
  void solicitarEntrega_deberiaRechazarDatosNulos() {
    DatosEntregaLogistica sinDonacion = datos(null);

    assertThrows(ValidationException.class, () -> broker.solicitarEntrega(null));
    assertThrows(ValidationException.class, () -> broker.solicitarEntrega(sinDonacion));
  }

  @Test
  void publicado_deberiaMarcarLaSolicitudComoEnviada() {
    broker.solicitarEntrega(datos(UUID.randomUUID()));
    EntradaOutboxLogistica entrada = unicaPendiente();

    broker.registrarResultado(entrada.getId(), ResultadoEnvio.PUBLICADO);

    assertEquals(EstadoSolicitudEntrega.ENVIADA, unicaSolicitud().getEstado());
    assertEquals(EstadoEntradaOutbox.PUBLICADO, entrada.getEstado());
    assertTrue(outbox.pendientesListas(AHORA.plusYears(1)).isEmpty());
  }

  @Test
  void rechazado_deberiaDescartarAlProveedorYPasarAlSiguiente() {
    broker.solicitarEntrega(datos(UUID.randomUUID()));
    EntradaOutboxLogistica primera = unicaPendiente();

    broker.registrarResultado(primera.getId(), ResultadoEnvio.RECHAZADO);

    assertEquals(EstadoEntradaOutbox.FALLIDO, primera.getEstado());
    SolicitudEntrega solicitud = unicaSolicitud();
    assertEquals(EstadoSolicitudEntrega.PENDIENTE, solicitud.getEstado());
    assertTrue(solicitud.fueDescartado("donatrack"));
    assertEquals("externo", solicitud.getProveedorActual());
    EntradaOutboxLogistica segunda = unicaPendiente();
    assertEquals("externo", segunda.getProveedorId());
    assertEquals(primera.getTraceId(), segunda.getTraceId());
  }

  @Test
  void rechazado_deberiaProgramarUnaNuevaRondaConEspera_CuandoTodosLosProveedoresRechazaron() {
    broker.solicitarEntrega(datos(UUID.randomUUID()));
    broker.registrarResultado(unicaPendiente().getId(), ResultadoEnvio.RECHAZADO);

    broker.registrarResultado(unicaPendiente().getId(), ResultadoEnvio.RECHAZADO);

    SolicitudEntrega solicitud = unicaSolicitud();
    assertEquals(EstadoSolicitudEntrega.PENDIENTE, solicitud.getEstado());
    assertEquals(2, solicitud.getRonda());
    assertTrue(solicitud.getProveedoresDescartados().isEmpty());
    assertEquals("donatrack", solicitud.getProveedorActual());
    EntradaOutboxLogistica nuevaRonda = unicaPendiente();
    assertEquals("donatrack", nuevaRonda.getProveedorId());
    assertEquals(AHORA.plusSeconds(60), nuevaRonda.getProximoIntento());
    assertFalse(nuevaRonda.estaListaPara(AHORA));
  }

  @Test
  void rechazado_deberiaMarcarFallida_CuandoSeAgotanLasRondas() {
    broker = brokerCon(2);
    broker.solicitarEntrega(datos(UUID.randomUUID()));

    for (int i = 0; i < 4; i++) {
      broker.registrarResultado(unicaPendiente().getId(), ResultadoEnvio.RECHAZADO);
    }

    assertEquals(EstadoSolicitudEntrega.FALLIDA, unicaSolicitud().getEstado());
    assertTrue(outbox.pendientesListas(AHORA.plusYears(1)).isEmpty());
  }

  @Test
  void incierto_deberiaReintentarConElMismoProveedorYNuncaPasarAOtro() {
    broker.solicitarEntrega(datos(UUID.randomUUID()));
    EntradaOutboxLogistica entrada = unicaPendiente();

    broker.registrarResultado(entrada.getId(), ResultadoEnvio.INCIERTO);

    EntradaOutboxLogistica pendiente = unicaPendiente();
    assertSame(entrada, pendiente);
    assertEquals("donatrack", pendiente.getProveedorId());
    assertEquals(1, pendiente.getIntentos());
    assertEquals(AHORA.plusSeconds(60), pendiente.getProximoIntento());
    SolicitudEntrega solicitud = unicaSolicitud();
    assertEquals(EstadoSolicitudEntrega.PENDIENTE, solicitud.getEstado());
    assertFalse(solicitud.fueDescartado("donatrack"));
  }

  @Test
  void incierto_deberiaMarcarFallidaSinProbarOtroProveedor_CuandoSeAgotanLosIntentos() {
    broker = brokerCon(2);
    broker.solicitarEntrega(datos(UUID.randomUUID()));
    EntradaOutboxLogistica entrada = unicaPendiente();

    broker.registrarResultado(entrada.getId(), ResultadoEnvio.INCIERTO);
    broker.registrarResultado(entrada.getId(), ResultadoEnvio.INCIERTO);

    assertEquals(EstadoEntradaOutbox.FALLIDO, entrada.getEstado());
    SolicitudEntrega solicitud = unicaSolicitud();
    assertEquals(EstadoSolicitudEntrega.FALLIDA, solicitud.getEstado());
    assertEquals("donatrack", solicitud.getProveedorActual());
    assertTrue(outbox.pendientesListas(AHORA.plusYears(1)).isEmpty());
  }

  @Test
  void errorDeContrato_deberiaMarcarFallidaSinReintentarNiReenviar() {
    broker.solicitarEntrega(datos(UUID.randomUUID()));
    EntradaOutboxLogistica entrada = unicaPendiente();

    broker.registrarResultado(entrada.getId(), ResultadoEnvio.ERROR_CONTRATO);

    assertEquals(EstadoEntradaOutbox.FALLIDO, entrada.getEstado());
    assertEquals(EstadoSolicitudEntrega.FALLIDA, unicaSolicitud().getEstado());
    assertTrue(outbox.pendientesListas(AHORA.plusYears(1)).isEmpty());
  }

  @Test
  void registrarResultado_deberiaIgnorarResultados_CuandoLaEntradaYaFueResuelta() {
    broker.solicitarEntrega(datos(UUID.randomUUID()));
    EntradaOutboxLogistica entrada = unicaPendiente();
    broker.registrarResultado(entrada.getId(), ResultadoEnvio.PUBLICADO);

    broker.registrarResultado(entrada.getId(), ResultadoEnvio.RECHAZADO);

    assertEquals(EstadoEntradaOutbox.PUBLICADO, entrada.getEstado());
    assertEquals(EstadoSolicitudEntrega.ENVIADA, unicaSolicitud().getEstado());
  }

  @Test
  void registrarResultado_deberiaIgnorarEntradasInexistentes() {
    UUID inexistente = UUID.randomUUID();

    assertDoesNotThrow(() -> broker.registrarResultado(inexistente, ResultadoEnvio.PUBLICADO));
  }

  @Test
  void registrarResultado_deberiaRechazarArgumentosNulos() {
    UUID entradaId = UUID.randomUUID();

    assertThrows(
        ValidationException.class, () -> broker.registrarResultado(null, ResultadoEnvio.PUBLICADO));
    assertThrows(ValidationException.class, () -> broker.registrarResultado(entradaId, null));
  }

  // ===================== Cancelación (HB-19) =====================

  private EntradaOutboxLogistica solicitudEnviadaConCancelacionPedida(UUID donacionId) {
    broker.solicitarEntrega(datos(donacionId));
    broker.registrarResultado(unicaPendiente().getId(), ResultadoEnvio.PUBLICADO);
    MDC.put(FeignTraceRequestInterceptor.MDC_TRACE_KEY, "trace-de-la-baja");
    assertEquals(ResultadoPedidoCancelacion.PEDIDA, broker.pedirCancelacion(donacionId));
    return unicaPendiente();
  }

  @Test
  void pedirCancelacion_sinSolicitudActiva_respondeSinEntregaYNoEncolaNada() {
    assertEquals(
        ResultadoPedidoCancelacion.SIN_ENTREGA, broker.pedirCancelacion(UUID.randomUUID()));
    assertTrue(outbox.pendientesListas(AHORA.plusYears(1)).isEmpty());
  }

  @Test
  void pedirCancelacion_conProveedorQueNoCancela_respondeNoSoportada() {
    when(catalogo.admiteCancelacion("donatrack")).thenReturn(false);
    UUID donacionId = UUID.randomUUID();
    broker.solicitarEntrega(datos(donacionId));
    broker.registrarResultado(unicaPendiente().getId(), ResultadoEnvio.PUBLICADO);

    assertEquals(ResultadoPedidoCancelacion.NO_SOPORTADA, broker.pedirCancelacion(donacionId));
    assertTrue(outbox.pendientesListas(AHORA.plusYears(1)).isEmpty());
  }

  @Test
  void pedirCancelacion_encolaLaCancelacionConElTraceIdDeLaBajaYSuDestino() {
    UUID donacionId = UUID.randomUUID();

    EntradaOutboxLogistica cancelacion = solicitudEnviadaConCancelacionPedida(donacionId);

    assertTrue(cancelacion.esCancelacion());
    assertEquals("trace-de-la-baja", cancelacion.getTraceId());
    assertEquals(donacionId, cancelacion.getDatos().donacionIndependienteId());
    assertEquals(Optional.of("donatrack"), broker.destinoDeCancelacion(cancelacion.getId()));
  }

  @Test
  void destinoDeCancelacion_conLaSolicitudTodaviaPendiente_laPosponeSinGastarIntentos() {
    UUID donacionId = UUID.randomUUID();
    broker.solicitarEntrega(datos(donacionId));
    broker.pedirCancelacion(donacionId);
    EntradaOutboxLogistica cancelacion =
        outbox.pendientesListas(AHORA.plusYears(1)).stream()
            .filter(EntradaOutboxLogistica::esCancelacion)
            .findFirst()
            .orElseThrow();

    assertTrue(broker.destinoDeCancelacion(cancelacion.getId()).isEmpty());

    assertTrue(cancelacion.estaPendiente());
    assertEquals(0, cancelacion.getIntentos());
    assertFalse(cancelacion.estaListaPara(AHORA));
  }

  @Test
  void destinoDeCancelacion_conLaSolicitudFallida_laDescarta() {
    UUID donacionId = UUID.randomUUID();
    broker.solicitarEntrega(datos(donacionId));
    EntradaOutboxLogistica pedido = unicaPendiente();
    broker.pedirCancelacion(donacionId);
    broker.registrarResultado(pedido.getId(), ResultadoEnvio.ERROR_CONTRATO);
    EntradaOutboxLogistica cancelacion = unicaPendiente();

    assertTrue(broker.destinoDeCancelacion(cancelacion.getId()).isEmpty());
    assertEquals(EstadoEntradaOutbox.FALLIDO, cancelacion.getEstado());
  }

  @Test
  void resultadoDeLaCancelacion_rechazoReintentaConElMismoProveedorSinTocarLaSolicitud() {
    EntradaOutboxLogistica cancelacion = solicitudEnviadaConCancelacionPedida(UUID.randomUUID());

    broker.registrarResultado(cancelacion.getId(), ResultadoEnvio.RECHAZADO);

    assertTrue(cancelacion.estaPendiente());
    assertEquals(1, cancelacion.getIntentos());
    assertEquals("donatrack", cancelacion.getProveedorId());
    assertEquals(EstadoSolicitudEntrega.ENVIADA, unicaSolicitud().getEstado());
    assertTrue(unicaSolicitud().getProveedoresDescartados().isEmpty());
  }

  @Test
  void resultadoDeLaCancelacion_publicadoYErrorDeContratoCierranLaEntrada() {
    EntradaOutboxLogistica publicada = solicitudEnviadaConCancelacionPedida(UUID.randomUUID());
    broker.registrarResultado(publicada.getId(), ResultadoEnvio.PUBLICADO);
    assertEquals(EstadoEntradaOutbox.PUBLICADO, publicada.getEstado());
    assertEquals(EstadoSolicitudEntrega.ENVIADA, unicaSolicitud().getEstado());

    broker.pedirCancelacion(unicaSolicitud().getDonacionIndependienteId());
    EntradaOutboxLogistica fallida = unicaPendiente();
    broker.registrarResultado(fallida.getId(), ResultadoEnvio.ERROR_CONTRATO);
    assertEquals(EstadoEntradaOutbox.FALLIDO, fallida.getEstado());
  }

  @Test
  void cancelacionConfirmada_cierraLaSolicitudYPermiteUnaNuevaSiSeReasigna() {
    UUID donacionId = UUID.randomUUID();
    solicitudEnviadaConCancelacionPedida(donacionId);

    broker.registrarCancelacionConfirmada(donacionId);
    broker.registrarCancelacionConfirmada(donacionId);

    assertEquals(EstadoSolicitudEntrega.CANCELADA, unicaSolicitud().getEstado());
    broker.solicitarEntrega(datos(donacionId));
    assertEquals(2, solicitudes.findAll().size());
    assertTrue(solicitudes.findActivaPorDonacion(donacionId).isPresent());
  }
}
