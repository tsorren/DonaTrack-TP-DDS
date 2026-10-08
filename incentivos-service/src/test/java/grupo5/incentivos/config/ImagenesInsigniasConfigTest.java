package grupo5.incentivos.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.incentivos.infrastructure.imagenes.MinioInsigniasSeeder;
import grupo5.incentivos.models.storage.IImagenesInsignias;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ImagenesInsigniasConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(ImagenesInsigniasConfig.class);

  @Test
  void sinLaPropiedadNoDeberiaRegistrarNadaDeMinio() {
    runner.run(
        contexto -> {
          assertTrue(contexto.getBeansOfType(IImagenesInsignias.class).isEmpty());
          assertTrue(contexto.getBeansOfType(MinioClient.class).isEmpty());
          assertTrue(contexto.getBeansOfType(MinioInsigniasSeeder.class).isEmpty());
        });
  }

  @Test
  void conLaPropiedadEnFalseNoDeberiaRegistrarNadaDeMinio() {
    runner
        .withPropertyValues("incentivos.minio.enabled=false")
        .run(contexto -> assertTrue(contexto.getBeansOfType(IImagenesInsignias.class).isEmpty()));
  }

  @Test
  void habilitadoDeberiaRegistrarClienteResolvedorYSeed() {
    runner
        .withPropertyValues(
            "incentivos.minio.enabled=true",
            "incentivos.minio.endpoint=http://minio:9000",
            "incentivos.minio.public-base-url=http://localhost:9000",
            "incentivos.minio.access-key=clave",
            "incentivos.minio.secret-key=secreto",
            "incentivos.minio.bucket=insignias")
        .run(
            contexto -> {
              assertEquals(1, contexto.getBeansOfType(MinioClient.class).size());
              assertEquals(1, contexto.getBeansOfType(MinioInsigniasSeeder.class).size());
              assertEquals(
                  "http://localhost:9000/insignias/explorador.png",
                  contexto
                      .getBean(IImagenesInsignias.class)
                      .urlPublica("/insignias/explorador.png"));
            });
  }
}
