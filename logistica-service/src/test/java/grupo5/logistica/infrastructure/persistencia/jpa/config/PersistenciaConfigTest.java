package grupo5.logistica.infrastructure.persistencia.jpa.config;

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
        new String[] {"grupo5.logistica.infrastructure.persistencia.jpa.repositories"},
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

  @Test
  void repositoriosEnMemoria_debenTenerPerfilNoPostgresNiMongo() {
    java.util.List<Class<?>> inMemoryRepos =
        java.util.List.of(
            grupo5.logistica.models.repositories.impl.CamionRepository.class,
            grupo5.logistica.models.repositories.impl.ChoferesRepository.class,
            grupo5.logistica.models.repositories.impl.EntregasRepository.class,
            grupo5.logistica.models.repositories.impl.EventosEntregaRepository.class,
            grupo5.logistica.models.repositories.impl.RutasRepository.class,
            grupo5.logistica.models.repositories.impl.SolicitudPlanificacionRepository.class,
            grupo5.logistica.models.repositories.impl.SolicitudesTransicionEntregaRepository.class);

    for (Class<?> repo : inMemoryRepos) {
      Profile profile = repo.getAnnotation(Profile.class);
      assertNotNull(profile, "El repositorio " + repo.getSimpleName() + " debe tener @Profile");
      assertArrayEquals(
          new String[] {"!postgres & !mongo"},
          profile.value(),
          "El repositorio "
              + repo.getSimpleName()
              + " debe tener @Profile(\"!postgres & !mongo\")");
    }
  }
}
