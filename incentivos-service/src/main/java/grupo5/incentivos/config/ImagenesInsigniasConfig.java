package grupo5.incentivos.config;

import grupo5.incentivos.infrastructure.imagenes.MinioImagenesInsigniasAdapter;
import grupo5.incentivos.infrastructure.imagenes.MinioInsigniasSeeder;
import grupo5.incentivos.models.storage.IImagenesInsignias;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integración con MinIO para las imágenes de insignias. Apagada por defecto: sin la propiedad (modo
 * en memoria, tests) no hay cliente ni seed y los mappers usan {@link
 * IImagenesInsignias#IDENTIDAD}.
 */
@Configuration
@ConditionalOnProperty(name = "incentivos.minio.enabled", havingValue = "true")
public class ImagenesInsigniasConfig {

  @Bean
  public MinioClient minioClient(
      @Value("${incentivos.minio.endpoint}") String endpoint,
      @Value("${incentivos.minio.access-key}") String accessKey,
      @Value("${incentivos.minio.secret-key}") String secretKey) {
    return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
  }

  @Bean
  public IImagenesInsignias imagenesInsignias(
      @Value("${incentivos.minio.public-base-url}") String publicBaseUrl,
      @Value("${incentivos.minio.bucket}") String bucket) {
    return new MinioImagenesInsigniasAdapter(publicBaseUrl, bucket);
  }

  @Bean
  public MinioInsigniasSeeder minioInsigniasSeeder(
      MinioClient minioClient, @Value("${incentivos.minio.bucket}") String bucket) {
    return new MinioInsigniasSeeder(minioClient, bucket);
  }
}
