package grupo5.logistica.infrastructure.persistencia.mongo.config; // NOSONAR

import static grupo5.logistica.config.RabbitMQConfig.EXCHANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.config.PlanificacionConfig;
import grupo5.logistica.config.RabbitMQConfig;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.infrastructure.ComunicadorEventosLogisticaRabbit;
import grupo5.logistica.infrastructure.GeneradorDeURLSeguimiento;
import grupo5.logistica.infrastructure.LogisticaEventPublisher;
import grupo5.logistica.infrastructure.persistencia.mongo.MongoDePrueba;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.CamionRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.ChoferesRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.EntregasRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.EventosEntregaRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.RutasRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.SolicitudPlanificacionRepositoryMongoAdapter;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.CamionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.ChoferMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.RutaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.SolicitudPlanificacionMongoMapper;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.IEventosEntregaRepository;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import grupo5.logistica.services.impl.PlanificacionService;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import grupo5.logistica.testutils.CamionMother;
import grupo5.logistica.testutils.ChoferMother;
import grupo5.logistica.testutils.EntregaMother;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Verifica contra un MongoDB real (replica set) que {@code @Transactional} funciona con el {@code
 * MongoTransactionManager}: un fallo a mitad de {@code persistirPlanificacion} no deja nada
 * escrito, un callback repetido no duplica rutas y los eventos a Rabbit salen solo después del
 * commit.
 */
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringJUnitConfig(
    classes = PlanificacionTransaccionesMongoTest.Contexto.class,
    initializers = ConfigDataApplicationContextInitializer.class)
@TestPropertySource(
    properties = {
      "logistica.tracking.base-url=http://localhost/seguimiento",
      "logistica.token-vuelta=token-sintetico"
    })
class PlanificacionTransaccionesMongoTest {

  private static final String BASE = "transacciones";
  private static final String COLECCION_RUTAS = "rutas";
  private static final String COLECCION_EVENTOS = "eventos_entrega";
  private static final LocalDate FECHA_PLANIFICADA = LocalDate.of(2030, 1, 1);
  private static final String CALLBACK_URL = "http://localhost/callback";
  private static final IllegalStateException FALLO_SIMULADO = new IllegalStateException("fallo");

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    MongoDePrueba.conectar(registry, BASE);
  }

  @Import({
    MongoDePrueba.Infraestructura.class,
    PlanificacionConfig.class,
    PlanificacionService.class,
    SolicitudPlanificacionMapper.class,
    ComunicadorEventosLogisticaRabbit.class,
    LogisticaEventPublisher.class,
    GeneradorDeURLSeguimiento.class,
    CamionRepositoryMongoAdapter.class,
    ChoferesRepositoryMongoAdapter.class,
    EntregasRepositoryMongoAdapter.class,
    EventosEntregaRepositoryMongoAdapter.class,
    RutasRepositoryMongoAdapter.class,
    SolicitudPlanificacionRepositoryMongoAdapter.class,
    CamionMongoMapper.class,
    ChoferMongoMapper.class,
    EntregaMongoMapper.class,
    RutaMongoMapper.class,
    SolicitudPlanificacionMongoMapper.class
  })
  @EnableTransactionManagement(proxyTargetClass = true)
  static class Contexto {}

  @MockitoBean RabbitTemplate rabbitTemplate;
  @MockitoBean IServicioExternoPlanificacion planificadorExterno;
  @MockitoSpyBean IEventosEntregaRepository eventos;

  @Autowired PlanificacionService planificacionService;
  @Autowired MongoTemplate mongoTemplate;
  @Autowired CamionRepositoryMongoAdapter camiones;
  @Autowired ChoferesRepositoryMongoAdapter choferes;
  @Autowired EntregasRepositoryMongoAdapter entregas;
  @Autowired SolicitudPlanificacionRepositoryMongoAdapter solicitudes;

  private MongoClient lectorIndependiente;
  private Camion camion;
  private Chofer chofer;
  private Entrega entrega;
  private SolicitudPlanificacion solicitud;

  @BeforeEach
  void prepararDatos() {
    MongoDePrueba.vaciar(mongoTemplate);
    reset(rabbitTemplate, eventos);
    lectorIndependiente = MongoClients.create(MongoDePrueba.uri(BASE));

    camion = camiones.save(CamionMother.disponible());
    chofer = choferes.save(ChoferMother.disponible());
    entrega = entregas.save(EntregaMother.pendiente());
    solicitud = solicitudes.save(solicitudPendiente());
  }

  @AfterEach
  void cerrarLector() {
    lectorIndependiente.close();
  }

  @Test
  void unFalloAlRegistrarLaRutaAsignada_noDejaRutasEventosNiEntregaModificada() {
    Long versionEntrega = entregaEnBase().getVersion();
    fallarAlRegistrarRutaAsignada();

    assertThrows(IllegalStateException.class, this::procesarCallback);

    assertEquals(0, mongoTemplate.getCollection(COLECCION_RUTAS).countDocuments());
    assertEquals(0, mongoTemplate.getCollection(COLECCION_EVENTOS).countDocuments());
    EntregaDocument enBase = entregaEnBase();
    assertNull(enBase.getAsignacion());
    assertEquals(versionEntrega, enBase.getVersion());
  }

  @Test
  void unFalloAlRegistrarLaRutaAsignada_dejaLaSolicitudPendienteYNoPublicaEnRabbit() {
    fallarAlRegistrarRutaAsignada();

    assertThrows(IllegalStateException.class, this::procesarCallback);

    assertEquals(EstadoSolicitud.PENDIENTE, estadoDeLaSolicitudEnBase());
    verifyNoInteractions(rabbitTemplate);
  }

  @Test
  void despuesDeUnFalloElCallbackSePuedeReintentar() {
    doThrow(FALLO_SIMULADO).doCallRealMethod().when(eventos).registrarRutaAsignada(any(), any());

    assertThrows(IllegalStateException.class, this::procesarCallback);
    procesarCallback();

    assertEquals(1, mongoTemplate.getCollection(COLECCION_RUTAS).countDocuments());
    assertEquals(EstadoSolicitud.PROCESADA, estadoDeLaSolicitudEnBase());
  }

  @Test
  void repetirUnCallbackProcesado_noDuplicaRutasNiEventos() {
    procesarCallback();
    procesarCallback();

    assertEquals(1, mongoTemplate.getCollection(COLECCION_RUTAS).countDocuments());
    assertEquals(1, mongoTemplate.getCollection(COLECCION_EVENTOS).countDocuments());
    String rutaAsignada = RabbitMQConfig.ROUTING_KEY_RUTA_ASIGNADA;
    verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(rutaAsignada), any(), conHeaders());
  }

  @Test
  void elEventoAMensajeriaSePublicaSoloDespuesDelCommit() {
    AtomicLong rutasVisiblesAlPublicar = new AtomicLong(-1);
    alPublicarEnRabbit(() -> rutasVisiblesAlPublicar.set(contarRutasDesdeOtroCliente()));

    procesarCallback();

    assertEquals(1, rutasVisiblesAlPublicar.get());
  }

  private void procesarCallback() {
    planificacionService.procesarCallback(callback());
  }

  private void fallarAlRegistrarRutaAsignada() {
    doThrow(FALLO_SIMULADO).when(eventos).registrarRutaAsignada(any(), any());
  }

  /** Corre la acción en el momento en que el publisher manda el mensaje a Rabbit. */
  private void alPublicarEnRabbit(Runnable accion) {
    Answer<Void> correrAccion = answerVoid(exchange -> accion.run());
    doAnswer(correrAccion).when(rabbitTemplate).convertAndSend(any(), any(), any(), conHeaders());
  }

  /** El publisher manda cada mensaje con un post-processor que le agrega los headers. */
  private static MessagePostProcessor conHeaders() {
    return any(MessagePostProcessor.class);
  }

  private EntregaDocument entregaEnBase() {
    return mongoTemplate.findById(entrega.getId(), EntregaDocument.class);
  }

  private EstadoSolicitud estadoDeLaSolicitudEnBase() {
    UUID id = solicitud.getId();
    return mongoTemplate.findById(id, SolicitudPlanificacionDocument.class).getEstado();
  }

  /** Cliente aparte: no comparte sesión ni transacción con el servicio, solo ve lo commiteado. */
  private long contarRutasDesdeOtroCliente() {
    return lectorIndependiente.getDatabase(BASE).getCollection(COLECCION_RUTAS).countDocuments();
  }

  private CallbackPlanificacionRequestDTO callback() {
    List<UUID> entregasDeLaRuta = List.of(entrega.getId());
    RutaPlanificadaDTO ruta =
        new RutaPlanificadaDTO(camion.getId(), chofer.getId(), FECHA_PLANIFICADA, entregasDeLaRuta);
    return new CallbackPlanificacionRequestDTO(solicitud.getId(), List.of(ruta), "OK", null);
  }

  private static SolicitudPlanificacion solicitudPendiente() {
    return new SolicitudPlanificacion(UUID.randomUUID(), FECHA_PLANIFICADA, 1, CALLBACK_URL);
  }
}
