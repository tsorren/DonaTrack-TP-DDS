package grupo5.logistica.infrastructure.persistencia.mongo.config; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.IEventosEntregaRepository;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import grupo5.logistica.services.impl.PlanificacionService;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifica contra un MongoDB real (replica set) que {@code @Transactional} funciona con el {@code
 * MongoTransactionManager}: un fallo a mitad de {@code persistirPlanificacion} no deja nada
 * escrito, un callback repetido no duplica rutas y los eventos a Rabbit salen solo después del
 * commit.
 */
@Testcontainers
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringJUnitConfig(
    classes = PlanificacionTransaccionesMongoTest.Contexto.class,
    initializers = ConfigDataApplicationContextInitializer.class)
@TestPropertySource(properties = "logistica.tracking.base-url=http://localhost/seguimiento")
class PlanificacionTransaccionesMongoTest {

  @Container static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("logistica"));
  }

  @Import({
    MongoAutoConfiguration.class,
    DataMongoAutoConfiguration.class,
    MongoPersistenciaConfig.class,
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

  private Camion camion;
  private Chofer chofer;
  private Entrega entrega;
  private SolicitudPlanificacion solicitud;

  @BeforeEach
  void prepararDatos() {
    mongoTemplate
        .getCollectionNames()
        .forEach(coleccion -> mongoTemplate.remove(new Query(), coleccion));
    reset(rabbitTemplate, eventosEntregaRepository);

    camion = camiones.save(new Camion("AB123CD", 20f, 5000f, 3f));
    chofer = choferes.save(new Chofer("Ada", "Lovelace", "LIC-1", "1111"));
    entrega = entregas.save(crearEntrega());
    solicitud =
        solicitudes.save(
            new SolicitudPlanificacion(
                UUID.randomUUID(), LocalDate.now().plusDays(1), 1, "http://localhost/callback"));
  }

  @Test
  void unFalloAMitadDePersistirPlanificacion_noDejaRutasNiEntregasModificadas() {
    Long versionEntrega =
        mongoTemplate.findById(entrega.getId(), EntregaDocument.class).getVersion();
    // Falla después de guardar la ruta y la entrega, durante la publicación de asignaciones
    doThrow(new IllegalStateException("fallo simulado"))
        .when(eventosEntregaRepository)
        .registrarRutaAsignada(any(), any());

    assertThrows(
        IllegalStateException.class, () -> planificacionService.procesarCallback(callback()));

    assertEquals(0, mongoTemplate.getCollection("rutas").countDocuments());
    assertEquals(0, mongoTemplate.getCollection("eventos_entrega").countDocuments());
    EntregaDocument enBase = mongoTemplate.findById(entrega.getId(), EntregaDocument.class);
    assertNull(enBase.getIdRuta());
    assertEquals(versionEntrega, enBase.getVersion());
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

    assertEquals(1, mongoTemplate.getCollection("rutas").countDocuments());
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

    assertEquals(1, mongoTemplate.getCollection("rutas").countDocuments());
    assertEquals(1, mongoTemplate.getCollection("eventos_entrega").countDocuments());
    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(RabbitMQConfig.EXCHANGE),
            eq(RabbitMQConfig.ROUTING_KEY_RUTA_ASIGNADA),
            any(Object.class),
            any(MessagePostProcessor.class));
  }

  @Test
  void elEventoAMensajeriaSePublicaSoloDespuesDelCommit() {
    AtomicLong rutasVisiblesAlPublicar = new AtomicLong(-1);
    // Cliente aparte: no comparte sesion ni transaccion con el servicio, solo ve lo commiteado
    try (MongoClient lector = MongoClients.create(MONGO.getReplicaSetUrl("logistica"))) {
      doAnswer(
              invocacion -> {
                rutasVisiblesAlPublicar.set(
                    lector.getDatabase("logistica").getCollection("rutas").countDocuments());
                return null;
              })
          .when(rabbitTemplate)
          .convertAndSend(
              any(String.class),
              any(String.class),
              any(Object.class),
              any(MessagePostProcessor.class));

      planificacionService.procesarCallback(callback());
    }

    assertEquals(1, rutasVisiblesAlPublicar.get());
  }

  private CallbackPlanificacionRequestDTO callback() {
    return new CallbackPlanificacionRequestDTO(
        solicitud.getId(),
        List.of(
            new RutaPlanificadaDTO(
                camion.getId(),
                chofer.getId(),
                LocalDate.now().plusDays(1),
                List.of(entrega.getId()))),
        "OK",
        null);
  }

  private static Entrega crearEntrega() {
    Localidad localidad =
        new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion direccion = new Direccion("Calle", 123, null, null, "1000", localidad);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion, 10f, 2f);
  }
}
