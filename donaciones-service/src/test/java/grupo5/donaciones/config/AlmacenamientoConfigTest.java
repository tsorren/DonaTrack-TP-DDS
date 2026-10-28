package grupo5.donaciones.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AlmacenamientoConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(AlmacenamientoConfig.class);

  @Test
  void sinLaPropiedadNoDeberiaRegistrarElClienteDeMinio() {
    runner.run(contexto -> assertTrue(contexto.getBeansOfType(MinioClient.class).isEmpty()));
  }

  @Test
  void conLaPropiedadEnFalseNoDeberiaRegistrarElClienteDeMinio() {
    runner
        .withPropertyValues("donaciones.minio.enabled=false")
        .run(contexto -> assertTrue(contexto.getBeansOfType(MinioClient.class).isEmpty()));
  }

  @Test
  void habilitadoDeberiaRegistrarElClienteDeMinio() {
    runner
        .withPropertyValues(
            "donaciones.minio.enabled=true",
            "donaciones.minio.endpoint=http://minio:9000",
            "donaciones.minio.access-key=clave",
            "donaciones.minio.secret-key=secreto")
        .run(contexto -> assertEquals(1, contexto.getBeansOfType(MinioClient.class).size()));
  }
}
