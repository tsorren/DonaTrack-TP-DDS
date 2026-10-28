package grupo5.donaciones.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integración con MinIO para los padrones CSV y las fotos de bienes. Apagada por defecto: sin la
 * propiedad (modo en memoria, tests) no hay cliente. El perfil {@code postgres} la enciende.
 */
@Configuration
@ConditionalOnProperty(name = "donaciones.minio.enabled", havingValue = "true")
public class AlmacenamientoConfig {

  @Bean
  public MinioClient minioClient(
      @Value("${donaciones.minio.endpoint}") String endpoint,
      @Value("${donaciones.minio.access-key}") String accessKey,
      @Value("${donaciones.minio.secret-key}") String secretKey) {
    return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
  }
}
