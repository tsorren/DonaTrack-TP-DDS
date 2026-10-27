package grupo5.logistica.infrastructure.persistencia.mongo; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.LogisticaServiceApplication;
import grupo5.logistica.infrastructure.persistencia.mongo.adapters.EntregasRepositoryMongoAdapter;
import grupo5.logistica.models.repositories.IEntregasRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

/** Con el perfil mongo la aplicación completa arranca y queda conectada a MongoDB. */
@DisabledIfDockerUnavailable
@ActiveProfiles("mongo")
@SpringBootTest(classes = LogisticaServiceApplication.class)
class ContextoMongoIT {

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    MongoDePrueba.conectar(registry, "contexto");
  }

  @Autowired IEntregasRepository entregas;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void losRepositoriosDelDominioSonLosAdaptadoresDeMongo() {
    assertInstanceOf(EntregasRepositoryMongoAdapter.class, entregas);
  }

  @Test
  void lasTransaccionesLasManejaMongo() {
    assertInstanceOf(MongoTransactionManager.class, transactionManager);
  }
}
