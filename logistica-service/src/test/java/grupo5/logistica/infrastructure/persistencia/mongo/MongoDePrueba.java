package grupo5.logistica.infrastructure.persistencia.mongo; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.config.MongoPersistenciaConfig;
import java.util.Set;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Un único MongoDB 7.0 en replica set para todos los tests de persistencia Mongo del módulo. Cada
 * clase de test usa su propia base: las clases corren en paralelo y así no se pisan los datos.
 */
public final class MongoDePrueba {

  private static final MongoDBContainer CONTENEDOR = iniciar();

  private MongoDePrueba() {}

  /** Autoconfiguración de Mongo más la configuración del perfil {@code mongo}. */
  @Import({
    MongoAutoConfiguration.class,
    DataMongoAutoConfiguration.class,
    MongoPersistenciaConfig.class
  })
  public static class Infraestructura {}

  public static void conectar(DynamicPropertyRegistry registry, String base) {
    registry.add("spring.mongodb.uri", () -> uri(base));
  }

  public static String uri(String base) {
    return CONTENEDOR.getReplicaSetUrl(base);
  }

  /** Borra los documentos sin borrar las colecciones, para no perder los índices. */
  public static void vaciar(MongoTemplate mongoTemplate) {
    Set<String> colecciones = mongoTemplate.getCollectionNames();
    colecciones.forEach(coleccion -> mongoTemplate.remove(new Query(), coleccion));
  }

  private static MongoDBContainer iniciar() {
    MongoDBContainer contenedor = new MongoDBContainer("mongo:7.0");
    contenedor.start();
    return contenedor;
  }
}
