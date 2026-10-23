package grupo5.logistica.infrastructure.persistencia.mongo.config; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Las fechas se guardan en UTC sin importar la zona horaria de la JVM, contra Mongo real. */
@Testcontainers
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringJUnitConfig(
    classes = FechasUtcMongoTest.Contexto.class,
    initializers = ConfigDataApplicationContextInitializer.class)
class FechasUtcMongoTest {

  private static final TimeZone BUENOS_AIRES =
      TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
  private static final LocalDate FECHA = LocalDate.of(2026, 10, 23);

  @Container static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("logistica"));
  }

  @Import({
    MongoAutoConfiguration.class,
    DataMongoAutoConfiguration.class,
    MongoPersistenciaConfig.class
  })
  static class Contexto {}

  @Autowired MongoTemplate mongoTemplate;
  @Autowired SpringDataRutaMongoRepository rutas;

  private TimeZone zonaOriginal;

  @BeforeEach
  void preparar() {
    zonaOriginal = TimeZone.getDefault();
    mongoTemplate.remove(new Query(), RutaDocument.class);
  }

  @AfterEach
  void restaurarZona() {
    TimeZone.setDefault(zonaOriginal);
  }

  @Test
  void unaRutaGuardadaEnBuenosAiresSeEncuentraPorFechaDesdeUtc() {
    UUID id = guardarRutaCon(BUENOS_AIRES);

    TimeZone.setDefault(UTC);

    assertEquals(List.of(id), idsConFecha());
  }

  @Test
  void unaRutaGuardadaEnUtcSeEncuentraPorFechaDesdeBuenosAires() {
    UUID id = guardarRutaCon(UTC);

    TimeZone.setDefault(BUENOS_AIRES);

    assertEquals(List.of(id), idsConFecha());
  }

  @Test
  void laFechaQuedaGuardadaComoMedianocheUtc() {
    guardarRutaCon(BUENOS_AIRES);

    Date guardada = mongoTemplate.getCollection("rutas").find().first().getDate("fecha");

    assertEquals(Instant.parse("2026-10-23T00:00:00Z"), guardada.toInstant());
  }

  private UUID guardarRutaCon(TimeZone zona) {
    TimeZone.setDefault(zona);
    UUID id = UUID.randomUUID();
    rutas.save(RutaDocument.builder().id(id).fecha(FECHA).build());
    return id;
  }

  private List<UUID> idsConFecha() {
    return rutas.findByFecha(FECHA).stream().map(RutaDocument::getId).toList();
  }
}
