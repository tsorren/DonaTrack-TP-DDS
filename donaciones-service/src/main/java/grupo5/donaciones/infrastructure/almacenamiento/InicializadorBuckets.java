package grupo5.donaciones.infrastructure.almacenamiento;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Al arrancar crea los buckets de Donaciones si no existen. Es idempotente y los deja privados: los
 * padrones solo los lee el servicio y las fotos se acceden con URLs prefirmadas. Si MinIO no
 * responde, lo registra y el servicio arranca igual (como el seed de insignias de Incentivos).
 */
public class InicializadorBuckets implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(InicializadorBuckets.class);

  private final MinioClient cliente;
  private final List<String> buckets;

  public InicializadorBuckets(MinioClient cliente, List<String> buckets) {
    this.cliente = cliente;
    this.buckets = List.copyOf(buckets);
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      crearBuckets();
    } catch (MinioException | IOException | GeneralSecurityException | RuntimeException e) {
      log.error("No se pudieron crear los buckets de Donaciones {} en MinIO", buckets, e);
    }
  }

  void crearBuckets() throws MinioException, IOException, GeneralSecurityException {
    for (String bucket : buckets) {
      crearSiFalta(bucket);
    }
  }

  private void crearSiFalta(String bucket)
      throws MinioException, IOException, GeneralSecurityException {
    if (cliente.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
      return;
    }
    try {
      cliente.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      log.info("Bucket '{}' creado en MinIO", bucket);
    } catch (ErrorResponseException e) {
      // Otra réplica lo creó entre el chequeo y la creación.
      if (!"BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
        throw e;
      }
    }
  }
}
