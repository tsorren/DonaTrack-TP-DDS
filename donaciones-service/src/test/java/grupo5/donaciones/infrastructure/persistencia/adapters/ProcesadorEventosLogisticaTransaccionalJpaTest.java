package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.infrastructure.idempotency.EventoConsumido;
import grupo5.donaciones.infrastructure.logistica.ProcesadorEventosLogistica;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.LogisticaPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.SolicitudCambioEstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * El camino transaccional de {@link ProcesadorEventosLogistica} con un {@code TransactionTemplate}
 * real sobre Postgres: el cambio de estado de la donación y el registro del evento como consumido
 * se confirman o se deshacen juntos.
 */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  DireccionesPersistenciaMapper.class,
  DonacionesPersistenciaMapper.class,
  DonacionRepositoryJpaAdapter.class,
  ItemsNormalizadosPersistenciaMapper.class,
  ItemDonacionNormalizadoRepositoryJpaAdapter.class,
  NecesidadesPersistenciaMapper.class,
  NecesidadRepositoryJpaAdapter.class,
  DonacionesIndependientesPersistenciaMapper.class,
  DonacionIndependienteRepositoryJpaAdapter.class,
  LogisticaPersistenciaMapper.class,
  EventosConsumidosRepositoryJpaAdapter.class,
  ProcesadorEventosLogistica.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class ProcesadorEventosLogisticaTransaccionalJpaTest {

  private static final String ACTOR = "admin@donatrack.org";
  private static final String ORIGEN = "cola-prueba";
  private static final String TIPO_EVENTO = "RutaAsignada";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private ProcesadorEventosLogistica procesador;
  @Autowired private IDonacionesIndependientesRepository independientes;
  @Autowired private IDonacionesRepository donaciones;
  @Autowired private IItemDonacionNormalizadoRepository itemsNormalizados;
  @Autowired private INecesidadesRepository necesidades;
  @Autowired private JdbcTemplate jdbc;

  // Aplica el cambio de estado sobre la base real, como lo hace el servicio de verdad
  @MockitoBean private IDonacionesIndependientesService servicio;
  @MockitoSpyBean private EventosConsumidosRepositoryJpaAdapter eventosConsumidos;

  private UUID donacionId;
  private UUID rutaId;

  @BeforeEach
  void preparar() {
    jdbc.update("DELETE FROM donaciones.evento_consumido");
    independientes.deleteAll();
    itemsNormalizados.deleteAll();
    donaciones.deleteAll();
    necesidades.deleteAll();
    jdbc.update("DELETE FROM donaciones.deposito");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.entidad_beneficiaria");
    jdbc.update("DELETE FROM donaciones.persona");
    jdbc.update("DELETE FROM donaciones.direccion");
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");

    donacionId = crearDonacionAsignada();
    rutaId = UUID.randomUUID();
    doAnswer(
            invocacion -> {
              CambioEstadoDonacionIndependienteRequestDTO pedido = invocacion.getArgument(1);
              DonacionIndependiente donacion =
                  independientes.findById(invocacion.getArgument(0)).orElseThrow();
              donacion.cambiarEstado(
                  new SolicitudCambioEstadoDonacionIndependiente(
                      pedido.estado(),
                      null,
                      pedido.justificacion(),
                      pedido.urlMapa(),
                      pedido.patenteCamion(),
                      pedido.replanificable(),
                      invocacion.getArgument(2)));
              independientes.save(donacion);
              return null;
            })
        .when(servicio)
        .cambiarEstado(any(), any(), any());
  }

  private UUID crearDonacionAsignada() {
    UUID categoriaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.categoria (id, nombre, con_uso, con_vencimiento, tipo_unidad)"
            + " VALUES (?, 'Alimentos', false, true, 'UNIDADES')",
        categoriaId);
    UUID subcategoriaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.subcategoria (id, id_categoria, nombre) VALUES (?, ?, 'Fideos')",
        subcategoriaId,
        categoriaId);
    UUID entidadId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'JURIDICA')", entidadId);
    jdbc.update(
        "INSERT INTO donaciones.juridica (id, razon_social, tipo_juridico, rubro)"
            + " VALUES (?, 'Comedor', 'ONG', 'Social')",
        entidadId);
    jdbc.update(
        "INSERT INTO donaciones.entidad_beneficiaria (id, activo) VALUES (?, true)", entidadId);
    UUID donanteId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'HUMANA')", donanteId);
    jdbc.update(
        "INSERT INTO donaciones.humana (id, nombre, apellido) VALUES (?, 'Ana', 'Prueba')",
        donanteId);
    jdbc.update("INSERT INTO donaciones.donante (id, activo) VALUES (?, true)", donanteId);

    Bien fideos = BienMother.alimentoPerecedero("Fideos secos");
    Donacion donacion = DonacionMother.segmentada(donanteId, fideos, 30);
    donaciones.save(donacion);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                donacion.getId(), BienMother.aceptado(fideos, subcategoriaId), 30));
    NecesidadRecurrente necesidad =
        NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);
    necesidades.save(necesidad);

    DonacionIndependiente independiente =
        new DonacionIndependiente(
            donacion.getId(),
            List.of(new ItemDonacionIndependiente(normalizado.getId(), normalizado.getBien(), 10)));
    independiente.asignar(ACTOR, necesidad.destinoDeAsignacion());
    independientes.save(independiente);
    return independiente.getId();
  }

  private EventoRutaAsignada evento() {
    return new EventoRutaAsignada(rutaId, donacionId, LocalDateTime.now());
  }

  private int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM donaciones." + tabla, Integer.class);
  }

  private TipoEstadoDonacion estadoActual() {
    return independientes.findById(donacionId).orElseThrow().getEstadoActual().getTipo();
  }

  @Test
  void procesar_elMismoEventoDosVeces_aplicaUnSoloCambioYLoRegistraUnaVez() {
    procesador.procesarRutaAsignada(evento(), ORIGEN);
    procesador.procesarRutaAsignada(evento(), ORIGEN);

    assertEquals(TipoEstadoDonacion.LISTA_PARA_ENTREGAR, estadoActual());
    // Un cambio por la asignación inicial y uno por el evento, no dos
    assertEquals(2, contar("cambio_estado_donacion_independiente"));
    assertEquals(1, contar("evento_consumido"));
    verify(servicio, times(1)).cambiarEstado(any(), any(), any());
  }

  @Test
  void procesar_siFallaElRegistroDelEvento_seDeshaceElCambioDeEstado() {
    doThrow(new IllegalStateException("falla simulada al registrar"))
        .when(eventosConsumidos)
        .registrar(any(EventoConsumido.class));

    procesador.procesarRutaAsignada(evento(), ORIGEN);

    // El cambio de estado ya se había guardado dentro de la transacción: tiene que haberse
    // revertido
    assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, estadoActual());
    assertEquals(1, contar("cambio_estado_donacion_independiente"));
    assertEquals(0, contar("evento_consumido"));
  }

  @Test
  void procesar_cuandoOtraEntregaDelMismoEventoSeAdelanta_laRestriccionUnicaDeshaceElCambio() {
    // Simula la carrera: la otra entrega ya registró el evento después de que ésta lo chequeó
    jdbc.update(
        "INSERT INTO donaciones.evento_consumido (id, event_type, queue_name, business_id,"
            + " id_donacion_independiente, fecha_procesamiento) VALUES (?, ?, ?, ?, ?, now())",
        UUID.randomUUID(),
        TIPO_EVENTO,
        ORIGEN,
        rutaId,
        donacionId);
    doReturn(false).when(eventosConsumidos).yaFueConsumido(TIPO_EVENTO, rutaId, donacionId);

    procesador.procesarRutaAsignada(evento(), ORIGEN);

    assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, estadoActual());
    assertEquals(1, contar("cambio_estado_donacion_independiente"));
    assertEquals(1, contar("evento_consumido"));
  }
}
