package grupo5.logistica.infrastructure.persistencia.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.logistica.infrastructure.persistencia.mongo.config.MongoPersistenciaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

class PersistenciaConfigTest {

  @Test
  void jpaPersistenciaConfig_debeTenerPerfilPostgresYRepositoriosJpa() {
    assertNotNull(JpaPersistenciaConfig.class.getAnnotation(Configuration.class));
    Profile profile = JpaPersistenciaConfig.class.getAnnotation(Profile.class);
    assertNotNull(profile);
    assertArrayEquals(new String[] {"postgres"}, profile.value());

    EnableJpaRepositories jpaRepos =
        JpaPersistenciaConfig.class.getAnnotation(EnableJpaRepositories.class);
    assertNotNull(jpaRepos);
    assertArrayEquals(
        new String[] {"grupo5.logistica.infrastructure.persistencia.repositories"},
        jpaRepos.basePackages());
  }

  @Test
  void mongoPersistenciaConfig_debeTenerPerfilMongoYRepositoriosMongo() {
    assertNotNull(MongoPersistenciaConfig.class.getAnnotation(Configuration.class));
    Profile profile = MongoPersistenciaConfig.class.getAnnotation(Profile.class);
    assertNotNull(profile);
    assertArrayEquals(new String[] {"mongo"}, profile.value());

    EnableMongoRepositories mongoRepos =
        MongoPersistenciaConfig.class.getAnnotation(EnableMongoRepositories.class);
    assertNotNull(mongoRepos);
    assertArrayEquals(
        new String[] {"grupo5.logistica.infrastructure.persistencia.mongo.repositories"},
        mongoRepos.basePackages());
  }
}
