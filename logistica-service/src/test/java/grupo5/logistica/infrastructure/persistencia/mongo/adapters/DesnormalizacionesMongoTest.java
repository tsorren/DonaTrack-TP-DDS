package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.mongo.config.MongoPersistenciaConfig;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.AsignacionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ParadaSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.CamionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.RutaMongoMapper;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Asignación embebida en la entrega y paradas embebidas en la ruta, contra un MongoDB real. */
@Testcontainers
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringJUnitConfig(
    classes = DesnormalizacionesMongoTest.Contexto.class,
    initializers = ConfigDataApplicationContextInitializer.class)
class DesnormalizacionesMongoTest {

  private static final LocalDate FECHA_RUTA = LocalDate.of(2030, 1, 1);
  private static final String PATENTE = "AB123CD";

  @Container static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("logistica"));
  }

  @Import({
    MongoAutoConfiguration.class,
    DataMongoAutoConfiguration.class,
    MongoPersistenciaConfig.class,
    CamionRepositoryMongoAdapter.class,
    EntregasRepositoryMongoAdapter.class,
    RutasRepositoryMongoAdapter.class,
    CamionMongoMapper.class,
    EntregaMongoMapper.class,
    RutaMongoMapper.class
  })
  static class Contexto {}

  @Autowired MongoTemplate mongoTemplate;
  @Autowired CamionRepositoryMongoAdapter camiones;
  @Autowired EntregasRepositoryMongoAdapter entregas;
  @Autowired RutasRepositoryMongoAdapter rutas;

  private Camion camion;

  @BeforeEach
  void limpiarYCrearCamion() {
    mongoTemplate
        .getCollectionNames()
        .forEach(coleccion -> mongoTemplate.remove(new Query(), coleccion));
    camion = camiones.save(new Camion(PATENTE, 20f, 5000f, 3f));
  }

  @Test
  void alAsignarUnaEntrega_quedaLaAsignacionCompletaEnElMismoDocumento() {
    Entrega entrega = entregas.save(nuevaEntrega());
    Ruta ruta = rutaCon(entrega);

    asignarYGuardar(ruta, entrega);

    AsignacionSubdocument asignacion = documentoDe(entrega).getAsignacion();
    assertEquals(ruta.getId(), asignacion.getIdRuta());
    assertEquals(FECHA_RUTA, asignacion.getFecha());
    assertEquals(camion.getId(), asignacion.getIdCamion());
    assertEquals(PATENTE, asignacion.getPatente());
  }

  @Test
  void laAsignacionConservaLaPatenteDelDiaAunqueDespuesCambie() {
    Entrega entrega = entregas.save(nuevaEntrega());
    asignarYGuardar(rutaCon(entrega), entrega);
    cambiarPatenteEnBase("ZZ999ZZ");
    Entrega asignada = entregas.findById(entrega.getId()).orElseThrow();
    asignada.iniciarRuta("Chofer");

    entregas.save(asignada);

    assertEquals(PATENTE, documentoDe(entrega).getAsignacion().getPatente());
  }

  @Test
  void lasParadasConservanElOrdenYCopianDonacionYDestino() {
    Entrega primera = entregas.save(nuevaEntrega());
    Entrega segunda = entregas.save(nuevaEntrega());
    Entrega tercera = entregas.save(nuevaEntrega());
    Ruta ruta = rutaCon(primera, segunda, tercera);

    rutas.save(ruta);

    List<ParadaSubdocument> paradas =
        mongoTemplate.findById(ruta.getId(), RutaDocument.class).getParadas();
    assertEquals(List.of(1, 2, 3), paradas.stream().map(ParadaSubdocument::getOrden).toList());
    assertEquals(segunda.getIdDonacion(), paradas.get(1).getIdDonacion());
    assertEquals("Callao", paradas.get(1).getDestino().getCalle());
    assertEquals(
        List.of(primera.getId(), segunda.getId(), tercera.getId()),
        rutas.findById(ruta.getId()).orElseThrow().getEntregaIds());
  }

  @Test
  void unaEntregaQueRegresaAlDeposito_quedaSinAsignacionYLaRutaConservaLaParada() {
    Entrega entrega = entregas.save(nuevaEntrega());
    Ruta ruta = rutaCon(entrega);
    asignarYGuardar(ruta, entrega);
    Entrega enRuta = entregas.findById(entrega.getId()).orElseThrow();
    enRuta.iniciarRuta("Chofer");
    enRuta.negarEntrega("Entidad", "No había nadie", true);
    enRuta.mandarARevision("Admin");
    enRuta.regresarAlDeposito("Admin");

    entregas.save(enRuta);

    assertNull(documentoDe(entrega).getAsignacion());
    assertEquals(
        List.of(entrega.getId()), entregas.findSinRuta().stream().map(Entrega::getId).toList());
    assertEquals(List.of(), entregas.findByRutaId(ruta.getId()));
    assertEquals(
        List.of(entrega.getId()), rutas.findById(ruta.getId()).orElseThrow().getEntregaIds());
  }

  @Test
  void findByRutaId_buscaPorLaAsignacionEmbebida() {
    Entrega entrega = entregas.save(nuevaEntrega());
    Ruta ruta = rutaCon(entrega);

    asignarYGuardar(ruta, entrega);

    assertEquals(
        List.of(entrega.getId()),
        entregas.findByRutaId(ruta.getId()).stream().map(Entrega::getId).toList());
    assertEquals(List.of(), entregas.findSinRuta());
  }

  private Ruta rutaCon(Entrega... entregasDeLaRuta) {
    Ruta ruta = new Ruta(FECHA_RUTA, UUID.randomUUID(), camion.getId());
    for (Entrega entrega : entregasDeLaRuta) {
      ruta.agregarEntrega(entrega.getId());
    }
    return ruta;
  }

  /** Mismo orden que la planificación: primero la ruta, después la entrega. */
  private void asignarYGuardar(Ruta ruta, Entrega entrega) {
    rutas.save(ruta);
    Entrega guardada = entregas.findById(entrega.getId()).orElseThrow();
    guardada.asignarRuta(ruta.getId());
    entregas.save(guardada);
  }

  private EntregaDocument documentoDe(Entrega entrega) {
    return mongoTemplate.findById(entrega.getId(), EntregaDocument.class);
  }

  private void cambiarPatenteEnBase(String patente) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(camion.getId())),
        Update.update("patente", patente),
        CamionDocument.class);
  }

  private static Entrega nuevaEntrega() {
    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion destino = new Direccion("Callao", 500, null, null, "1022", caba);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), destino, 10f, 2f);
  }
}
