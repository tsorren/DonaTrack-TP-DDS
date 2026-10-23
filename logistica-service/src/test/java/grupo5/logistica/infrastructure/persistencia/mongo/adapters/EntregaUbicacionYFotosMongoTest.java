package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.mongo.config.MongoPersistenciaConfig;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.FotoRecepcionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.Instant;
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
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Ubicación GeoJSON, fotos de recepción e índices de la colección entregas, contra Mongo real. */
@Testcontainers
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringJUnitConfig(
    classes = EntregaUbicacionYFotosMongoTest.Contexto.class,
    initializers = ConfigDataApplicationContextInitializer.class)
class EntregaUbicacionYFotosMongoTest {

  private static final CoordenadasGeograficas OBELISCO =
      new CoordenadasGeograficas(-34.6037, -58.3816);
  private static final CoordenadasGeograficas CORDOBA =
      new CoordenadasGeograficas(-31.4201, -64.1888);

  @Container static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("logistica"));
  }

  @Import({
    MongoAutoConfiguration.class,
    DataMongoAutoConfiguration.class,
    MongoPersistenciaConfig.class,
    EntregasRepositoryMongoAdapter.class,
    EntregaMongoMapper.class
  })
  static class Contexto {}

  @Autowired MongoTemplate mongoTemplate;
  @Autowired EntregasRepositoryMongoAdapter entregas;

  @BeforeEach
  void limpiar() {
    mongoTemplate.remove(new Query(), EntregaDocument.class);
  }

  @Test
  void laColeccionEntregasTieneLosIndicesDeUbicacionBeneficiariaYRuta() {
    entregas.save(entregaEn(OBELISCO));

    List<IndexInfo> indices = mongoTemplate.indexOps(EntregaDocument.class).getIndexInfo();

    assertTrue(tieneIndice(indices, "destino.ubicacion"));
    assertTrue(tieneIndice(indices, "idBeneficiaria"));
    assertTrue(tieneIndice(indices, "asignacion.idRuta"));
  }

  @Test
  void unaConsultaNearSobreLaUbicacionDevuelveSoloLaEntregaCercana() {
    Entrega cercana = entregas.save(entregaEn(OBELISCO));
    entregas.save(entregaEn(CORDOBA));
    GeoJsonPoint plazaDeMayo = new GeoJsonPoint(-58.3702, -34.6083);

    List<EntregaDocument> encontradas =
        mongoTemplate.find(
            Query.query(
                Criteria.where("destino.ubicacion").nearSphere(plazaDeMayo).maxDistance(5_000)),
            EntregaDocument.class);

    assertEquals(
        List.of(cercana.getId()), encontradas.stream().map(EntregaDocument::getId).toList());
  }

  @Test
  void unaEntregaConTresFotosSeGuardaYSeLeeCompleta() {
    Entrega entrega = entregaEn(OBELISCO);
    entrega.iniciarRuta("Chofer");
    entrega.confirmarEntrega("Entidad", List.of("fotos/1.jpg", "fotos/2.jpg", "fotos/3.jpg"));

    entregas.save(entrega);

    assertEquals(
        List.of("fotos/1.jpg", "fotos/2.jpg", "fotos/3.jpg"),
        entregas.findById(entrega.getId()).orElseThrow().getFotosRecepcion());
  }

  @Test
  void adjuntarOtraFotoConservaLaHoraDeSubidaDeLasAnteriores() {
    Entrega entrega = entregaEn(OBELISCO);
    entrega.iniciarRuta("Chofer");
    entrega.confirmarEntrega("Entidad", List.of("fotos/1.jpg"));
    entregas.save(entrega);
    Instant subidaOriginal = fotosEnBase(entrega).getFirst().getSubidaEn();
    Entrega guardada = entregas.findById(entrega.getId()).orElseThrow();
    guardada.adjuntarFotosRecepcion(List.of("fotos/2.jpg"));

    entregas.save(guardada);

    List<FotoRecepcionSubdocument> fotos = fotosEnBase(entrega);
    assertEquals(2, fotos.size());
    assertEquals(subidaOriginal, fotos.getFirst().getSubidaEn());
  }

  private List<FotoRecepcionSubdocument> fotosEnBase(Entrega entrega) {
    return mongoTemplate.findById(entrega.getId(), EntregaDocument.class).getFotosRecepcion();
  }

  private static boolean tieneIndice(List<IndexInfo> indices, String campo) {
    return indices.stream().anyMatch(indice -> indice.isIndexForFields(List.of(campo)));
  }

  private static Entrega entregaEn(CoordenadasGeograficas coordenadas) {
    Localidad caba = new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
    Direccion destino = new Direccion("Callao", 500, null, null, "1022", caba, coordenadas);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), destino, 10f, 2f);
  }
}
