package grupo5.donaciones.config;

import grupo5.donaciones.infrastructure.almacenamiento.InicializadorBuckets;
import grupo5.donaciones.infrastructure.almacenamiento.MinioStorageAdapter;
import grupo5.donaciones.infrastructure.almacenamiento.StorageEnMemoria;
import grupo5.donaciones.models.ports.StoragePort;
import io.minio.MinioClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Almacenamiento de objetos para los padrones CSV y las fotos de bienes. Con {@code
 * donaciones.minio.enabled=true} (perfil {@code postgres}) usa MinIO y crea los buckets al
 * arrancar; si no, un {@link StoragePort} en memoria (modo en memoria, tests).
 */
@Configuration
@EnableConfigurationProperties(AlmacenamientoProperties.class)
public class AlmacenamientoConfig {

  static final String PROPIEDAD_MINIO = "donaciones.minio.enabled";

  @Configuration
  @ConditionalOnProperty(name = PROPIEDAD_MINIO, havingValue = "true")
  static class Minio {

    /** Región fija: con región conocida, firmar una URL no consulta a MinIO. */
    private static final String REGION = "us-east-1";

    @Bean
    MinioClient minioClient(
        @Value("${donaciones.minio.endpoint}") String endpoint,
        @Value("${donaciones.minio.access-key}") String accessKey,
        @Value("${donaciones.minio.secret-key}") String secretKey) {
      return MinioClient.builder()
          .endpoint(endpoint)
          .credentials(accessKey, secretKey)
          .region(REGION)
          .build();
    }

    @Bean
    StoragePort storagePort(
        MinioClient minioClient,
        @Value("${donaciones.minio.public-endpoint}") String publicEndpoint,
        @Value("${donaciones.minio.access-key}") String accessKey,
        @Value("${donaciones.minio.secret-key}") String secretKey) {
      MinioClient clienteFirmas =
          MinioClient.builder()
              .endpoint(publicEndpoint)
              .credentials(accessKey, secretKey)
              .region(REGION)
              .build();
      return new MinioStorageAdapter(minioClient, clienteFirmas);
    }

    @Bean
    InicializadorBuckets inicializadorBuckets(
        MinioClient minioClient, AlmacenamientoProperties propiedades) {
      return new InicializadorBuckets(
          minioClient, List.of(propiedades.bucketPadrones(), propiedades.bucketFotos()));
    }
  }

  @Configuration
  @ConditionalOnProperty(name = PROPIEDAD_MINIO, havingValue = "false", matchIfMissing = true)
  static class EnMemoria {

    @Bean
    StoragePort storagePort() {
      return new StorageEnMemoria();
    }
  }
}
