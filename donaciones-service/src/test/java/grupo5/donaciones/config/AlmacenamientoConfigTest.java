package grupo5.donaciones.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.infrastructure.almacenamiento.InicializadorBuckets;
import grupo5.donaciones.infrastructure.almacenamiento.MinioStorageAdapter;
import grupo5.donaciones.infrastructure.almacenamiento.StorageEnMemoria;
import grupo5.donaciones.models.ports.StoragePort;
import io.minio.MinioClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.unit.DataSize;

class AlmacenamientoConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(AlmacenamientoConfig.class);

  @Test
  void sinLaPropiedadDeberiaUsarElStorageEnMemoriaYNoRegistrarNadaDeMinio() {
    runner.run(
        contexto -> {
          assertInstanceOf(StorageEnMemoria.class, contexto.getBean(StoragePort.class));
          assertTrue(contexto.getBeansOfType(MinioClient.class).isEmpty());
          assertTrue(contexto.getBeansOfType(InicializadorBuckets.class).isEmpty());
        });
  }

  @Test
  void conLaPropiedadEnFalseDeberiaUsarElStorageEnMemoria() {
    runner
        .withPropertyValues("donaciones.minio.enabled=false")
        .run(
            contexto -> {
              assertInstanceOf(StorageEnMemoria.class, contexto.getBean(StoragePort.class));
              assertTrue(contexto.getBeansOfType(MinioClient.class).isEmpty());
            });
  }

  @Test
  void habilitadoDeberiaRegistrarElAdapterDeMinioYElInicializadorDeBuckets() {
    runner
        .withPropertyValues(
            "donaciones.minio.enabled=true",
            "donaciones.minio.endpoint=http://minio:9000",
            "donaciones.minio.public-endpoint=http://localhost:9000",
            "donaciones.minio.access-key=clave",
            "donaciones.minio.secret-key=secreto")
        .run(
            contexto -> {
              assertInstanceOf(MinioStorageAdapter.class, contexto.getBean(StoragePort.class));
              assertEquals(1, contexto.getBeansOfType(MinioClient.class).size());
              assertEquals(1, contexto.getBeansOfType(InicializadorBuckets.class).size());
            });
  }

  @Test
  void sinPropiedadesDeAlmacenamientoDeberiaUsarLosValoresPorDefecto() {
    runner.run(
        contexto -> {
          AlmacenamientoProperties propiedades = contexto.getBean(AlmacenamientoProperties.class);
          assertEquals("donaciones-padrones", propiedades.bucketPadrones());
          assertEquals("donaciones-fotos-bienes", propiedades.bucketFotos());
          assertEquals(DataSize.ofMegabytes(50), propiedades.csvTamanioMaximo());
          assertEquals(DataSize.ofMegabytes(10), propiedades.fotoTamanioMaximo());
          assertEquals(
              List.of("image/jpeg", "image/png", "image/webp"), propiedades.fotoTiposPermitidos());
          assertEquals(Duration.ofMinutes(15), propiedades.vigenciaSubida());
          assertEquals(Duration.ofHours(2), propiedades.vigenciaDescarga());
        });
  }
}
