package grupo5.logistica.infrastructure.persistencia.mongo.config; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
@TestPropertySource(properties = "logistica.tracking.base-url=http://localhost/seguimiento")
class PlanificacionTransaccionesMongoTest {

  private static final String BASE = "transacciones";
  private static final String COLECCION_RUTAS = "rutas";
  private static final String COLECCION_EVENTOS = "eventos_entrega";
  private static final LocalDate FECHA_PLANIFICADA = LocalDate.of(2030, 1, 1);

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
  @MockitoSpyBean IEventosEntregaRepository eventosEntregaRepository;

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
    reset(rabbitTemplate, eventosEntregaRepository);
    lectorIndependiente = MongoClients.create(MongoDePrueba.uri(BASE));

    camion = camiones.save(CamionMother.disponible());
    chofer = choferes.save(ChoferMother.disponible());
    entrega = entregas.save(EntregaMother.pendiente());
    solicitud =
        solicitudes.save(
            new SolicitudPlanificacion(
                UUID.randomUUID(), FECHA_PLANIFICADA, 1, "http://localhost/callback"));
  }

  @AfterEach
  void cerrarLector() {
    lectorIndependiente.close();
  }

  @Test
  void unFalloAlRegistrarLaRutaAsignada_noDejaRutasEventosNiEntregaModificada() {
    Long versionEntrega =
        mongoTemplate.findById(entrega.getId(), EntregaDocument.class).getVersion();
    fallarAlRegistrarRutaAsignada();

    assertThrows(
        IllegalStateException.class, () -> planificacionService.procesarCallback(callback()));

    assertEquals(0, mongoTemplate.getCollection(COLECCION_RUTAS).countDocuments());
    assertEquals(0, mongoTemplate.getCollection(COLECCION_EVENTOS).countDocuments());
    EntregaDocument enBase = mongoTemplate.findById(entrega.getId(), EntregaDocument.class);
    assertNull(enBase.getAsignacion());
    assertEquals(versionEntrega, enBase.getVersion());
  }

  @Test
  void unFalloAlRegistrarLaRutaAsignada_dejaLaSolicitudPendienteYNoPublicaEnRabbit() {
    fallarAlRegistrarRutaAsignada();

    assertThrows(
        IllegalStateException.class, () -> planificacionService.procesarCallback(callback()));

    assertEquals(
        EstadoSolicitud.PENDIENTE,
        mongoTemplate
            .findById(solicitud.getId(), SolicitudPlanificacionDocument.class)
            .getEstado());
    verifyNoInteractions(rabbitTemplate);
  }

  @Test
  void despuesDeUnFalloElCallbackSePuedeReintentar() {
    doThrow(new IllegalStateException("fallo simulado"))
        .doCallRealMethod()
        .when(eventosEntregaRepository)
        .registrarRutaAsignada(any(), any());

    assertThrows(
        IllegalStateException.class, () -> planificacionService.procesarCallback(callback()));
    planificacionService.procesarCallback(callback());

    assertEquals(1, mongoTemplate.getCollection(COLECCION_RUTAS).countDocuments());
    assertEquals(
        EstadoSolicitud.PROCESADA,
        mongoTemplate
            .findById(solicitud.getId(), SolicitudPlanificacionDocument.class)
            .getEstado());
  }

  @Test
  void repetirUnCallbackProcesado_noDuplicaRutasNiEventos() {
    planificacionService.procesarCallback(callback());
    planificacionService.procesarCallback(callback());

    assertEquals(1, mongoTemplate.getCollection(COLECCION_RUTAS).countDocuments());
    assertEquals(1, mongoTemplate.getCollection(COLECCION_EVENTOS).countDocuments());
    verify(rabbitTemplate)
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE),
            eq(RabbitMQConfig.ROUTING_KEY_RUTA_ASIGNADA),
            any(Object.class),
            any(MessagePostProcessor.class));
  }

  @Test
  void elEventoAMensajeriaSePublicaSoloDespuesDelCommit() {
    AtomicLong rutasVisiblesAlPublicar = new AtomicLong(-1);
    doAnswer(
            invocacion -> {
              rutasVisiblesAlPublicar.set(contarRutasDesdeOtroCliente());
              return null;
            })
        .when(rabbitTemplate)
        .convertAndSend(
            any(String.class),
            any(String.class),
            any(Object.class),
            any(MessagePostProcessor.class));

    planificacionService.procesarCallback(callback());

    assertEquals(1, rutasVisiblesAlPublicar.get());
  }

  private void fallarAlRegistrarRutaAsignada() {
    doThrow(new IllegalStateException("fallo simulado"))
        .when(eventosEntregaRepository)
        .registrarRutaAsignada(any(), any());
  }

  /** Cliente aparte: no comparte sesión ni transacción con el servicio, solo ve lo commiteado. */
  private long contarRutasDesdeOtroCliente() {
    return lectorIndependiente.getDatabase(BASE).getCollection(COLECCION_RUTAS).countDocuments();
  }

  private CallbackPlanificacionRequestDTO callback() {
    return new CallbackPlanificacionRequestDTO(
        solicitud.getId(),
        List.of(
            new RutaPlanificadaDTO(
                camion.getId(), chofer.getId(), FECHA_PLANIFICADA, List.of(entrega.getId()))),
        "OK",
        null);
  }
}
