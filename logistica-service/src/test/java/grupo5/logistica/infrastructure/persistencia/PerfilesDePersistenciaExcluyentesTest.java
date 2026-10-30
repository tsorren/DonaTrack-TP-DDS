package grupo5.logistica.infrastructure.persistencia; // NOSONAR

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PerfilesDePersistenciaExcluyentesTest {

  private final ApplicationContextRunner contexto =
      new ApplicationContextRunner().withUserConfiguration(PerfilesDePersistenciaExcluyentes.class);

  @Test
  void conPostgresYMongoActivos_elContextoNoArranca() {
    contexto
        .withInitializer(c -> c.getEnvironment().setActiveProfiles("postgres", "mongo"))
        .run(arrancado -> assertNotNull(arrancado.getStartupFailure()));
  }

  @Test
  void conUnSoloMotorActivo_elContextoArranca() {
    contexto
        .withInitializer(c -> c.getEnvironment().setActiveProfiles("mongo"))
        .run(arrancado -> assertNull(arrancado.getStartupFailure()));
  }
}
