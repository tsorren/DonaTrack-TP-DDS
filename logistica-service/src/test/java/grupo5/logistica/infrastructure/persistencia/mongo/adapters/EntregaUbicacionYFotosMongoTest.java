package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.mongo.MongoDePrueba;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.FotoRecepcionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.testutils.EntregaMother;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
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

/** Ubicación GeoJSON, fotos de recepción e índices de la colección entregas, contra Mongo real. */
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
  private static final Duration UNA_HORA = Duration.ofHours(1);

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    MongoDePrueba.conectar(registry, "ubicacion_y_fotos");
  }

  @Import({
    MongoDePrueba.Infraestructura.class,
    EntregasRepositoryMongoAdapter.class,
    EntregaMongoMapper.class
  })
  static class Contexto {

    @Bean
    RelojManual reloj() {
      return new RelojManual();
    }
  }

  @Autowired MongoTemplate mongoTemplate;
  @Autowired EntregasRepositoryMongoAdapter entregas;
  @Autowired RelojManual reloj;

  @BeforeEach
  void limpiar() {
    mongoTemplate.remove(new Query(), EntregaDocument.class);
  }

  @Test
  void laColeccionEntregasTieneLosIndicesDeUbicacionBeneficiariaYRuta() {
    entregas.save(EntregaMother.pendienteEn(OBELISCO));

    List<IndexInfo> indices = mongoTemplate.indexOps(EntregaDocument.class).getIndexInfo();

    assertTrue(tieneIndice(indices, "destino.ubicacion"));
    assertTrue(tieneIndice(indices, "idBeneficiaria"));
    assertTrue(tieneIndice(indices, "asignacion.idRuta"));
  }

  @Test
  void unaConsultaNearSobreLaUbicacionDevuelveSoloLaEntregaCercana() {
    Entrega cercana = entregas.save(EntregaMother.pendienteEn(OBELISCO));
    entregas.save(EntregaMother.pendienteEn(CORDOBA));
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
    Entrega entrega = EntregaMother.pendienteEn(OBELISCO);
    entrega.iniciarRuta("Chofer");
    entrega.confirmarEntrega("Entidad", List.of("fotos/1.jpg", "fotos/2.jpg", "fotos/3.jpg"));

    entregas.save(entrega);

    assertEquals(
        List.of("fotos/1.jpg", "fotos/2.jpg", "fotos/3.jpg"),
        entregas.findById(entrega.getId()).orElseThrow().getFotosRecepcion());
  }

  @Test
  void adjuntarOtraFotoMasTarde_conservaLaHoraDeSubidaDeLaAnterior() {
    Entrega entrega = guardarConfirmadaConUnaFoto();
    Instant primerGuardado = reloj.instant();
    reloj.avanzar(UNA_HORA);
    Entrega guardada = entregas.findById(entrega.getId()).orElseThrow();
    guardada.adjuntarFotosRecepcion(List.of("fotos/2.jpg"));

    entregas.save(guardada);

    assertEquals(primerGuardado, fotosEnBase(entrega).getFirst().getSubidaEn());
  }

  @Test
  void adjuntarOtraFotoMasTarde_registraLaHoraDelNuevoGuardado() {
    Entrega entrega = guardarConfirmadaConUnaFoto();
    reloj.avanzar(UNA_HORA);
    Entrega guardada = entregas.findById(entrega.getId()).orElseThrow();
    guardada.adjuntarFotosRecepcion(List.of("fotos/2.jpg"));

    entregas.save(guardada);

    assertEquals(reloj.instant(), fotosEnBase(entrega).getLast().getSubidaEn());
  }

  private Entrega guardarConfirmadaConUnaFoto() {
    Entrega entrega = EntregaMother.pendienteEn(OBELISCO);
    entrega.iniciarRuta("Chofer");
    entrega.confirmarEntrega("Entidad", List.of("fotos/1.jpg"));
    return entregas.save(entrega);
  }

  private List<FotoRecepcionSubdocument> fotosEnBase(Entrega entrega) {
    return mongoTemplate.findById(entrega.getId(), EntregaDocument.class).getFotosRecepcion();
  }

  private static boolean tieneIndice(List<IndexInfo> indices, String campo) {
    return indices.stream().anyMatch(indice -> indice.isIndexForFields(List.of(campo)));
  }

  /** Reloj que solo avanza cuando el test lo pide. */
  static class RelojManual extends Clock {

    private Instant ahora = Instant.parse("2026-10-23T10:00:00Z");

    void avanzar(Duration duracion) {
      ahora = ahora.plus(duracion);
    }

    @Override
    public Instant instant() {
      return ahora;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
      return this;
    }
  }
}
