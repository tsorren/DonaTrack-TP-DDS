package grupo5.logistica.config;

import grupo5.common.exceptions.InfrastructureException;
import grupo5.logistica.infrastructure.almacenamiento.AlmacenFotosEnMemoria;
import grupo5.logistica.infrastructure.almacenamiento.MinioAlmacenFotosRecepcion;
import grupo5.logistica.services.AlmacenFotosRecepcion;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Almacenamiento de las fotos de recepción (ADR-E6-08). Con {@code logistica.minio.enabled=true}
 * (docker compose y preprod) usa MinIO y crea el bucket al arrancar; si no, uno en memoria (modo en
 * memoria, tests).
 */
@Configuration
public class AlmacenFotosConfig {

  static final String PROPIEDAD_MINIO = "logistica.minio.enabled";

  @Configuration
  @ConditionalOnProperty(name = PROPIEDAD_MINIO, havingValue = "true")
  static class Minio {

    private static final Logger log = LoggerFactory.getLogger(Minio.class);

    /** Región fija: con región conocida, firmar una URL no consulta a MinIO. */
    private static final String REGION = "us-east-1";

    @Bean
    MinioAlmacenFotosRecepcion almacenFotosRecepcion(
        @Value("${logistica.minio.endpoint}") String endpoint,
        @Value("${logistica.minio.public-endpoint}") String publicEndpoint,
        @Value("${logistica.minio.access-key}") String accessKey,
        @Value("${logistica.minio.secret-key}") String secretKey) {
      return new MinioAlmacenFotosRecepcion(
          cliente(endpoint, accessKey, secretKey), cliente(publicEndpoint, accessKey, secretKey));
    }

    /**
     * Si MinIO no responde al arrancar, lo registra y el servicio arranca igual (como Donaciones).
     */
    @Bean
    ApplicationRunner inicializadorBucketFotos(MinioAlmacenFotosRecepcion almacen) {
      return args -> {
        try {
          almacen.crearBucketSiFalta();
        } catch (InfrastructureException e) {
          log.error("No se pudo crear el bucket {} en MinIO", MinioAlmacenFotosRecepcion.BUCKET, e);
        }
      };
    }

    private static MinioClient cliente(String endpoint, String accessKey, String secretKey) {
      return MinioClient.builder()
          .endpoint(endpoint)
          .credentials(accessKey, secretKey)
          .region(REGION)
          .build();
    }
  }

  @Configuration
  @ConditionalOnProperty(name = PROPIEDAD_MINIO, havingValue = "false", matchIfMissing = true)
  static class EnMemoria {

    @Bean
    AlmacenFotosRecepcion almacenFotosRecepcion() {
      return new AlmacenFotosEnMemoria();
    }
  }
}
