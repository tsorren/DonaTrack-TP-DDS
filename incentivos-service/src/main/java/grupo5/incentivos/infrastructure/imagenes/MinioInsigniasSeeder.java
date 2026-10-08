package grupo5.incentivos.infrastructure.imagenes;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Al arrancar deja listo el bucket de insignias: lo crea si no existe, le da lectura anónima de
 * descarga y sube las imágenes de {@code classpath:insignias/} que todavía no estén. Es
 * idempotente. Si MinIO no responde registra el error y el servicio sigue levantando (las URLs son
 * determinísticas, apuntarán a la imagen apenas el objeto exista).
 */
public class MinioInsigniasSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(MinioInsigniasSeeder.class);
  private static final String PATRON_IMAGENES = "classpath:insignias/*.png";
  private static final String CONTENT_TYPE = "image/png";

  private final MinioClient client;
  private final String bucket;

  public MinioInsigniasSeeder(MinioClient client, String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      sembrar();
    } catch (Exception e) {
      log.error("No se pudieron sembrar las imágenes de insignias en el bucket '{}'", bucket, e);
    }
  }

  void sembrar() throws Exception {
    asegurarBucketPublico();
    for (Resource imagen :
        new PathMatchingResourcePatternResolver().getResources(PATRON_IMAGENES)) {
      subirSiFalta(imagen);
    }
  }

  private void asegurarBucketPublico() throws Exception {
    if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
      try {
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        log.info("Bucket '{}' creado en MinIO", bucket);
      } catch (ErrorResponseException e) {
        // Otra réplica lo creó entre el chequeo y la creación.
        if (!"BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
          throw e;
        }
      }
    }
    client.setBucketPolicy(
        SetBucketPolicyArgs.builder().bucket(bucket).config(politicaLecturaAnonima()).build());
  }

  private void subirSiFalta(Resource imagen) throws Exception {
    String clave = imagen.getFilename();
    if (existe(clave)) {
      return;
    }
    try (InputStream contenido = imagen.getInputStream()) {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(clave).stream(
                  contenido, imagen.contentLength(), -1)
              .contentType(CONTENT_TYPE)
              .build());
    }
    log.info("Imagen de insignia '{}' subida al bucket '{}'", clave, bucket);
  }

  private boolean existe(String clave) throws Exception {
    try {
      client.statObject(StatObjectArgs.builder().bucket(bucket).object(clave).build());
      return true;
    } catch (ErrorResponseException e) {
      String codigo = e.errorResponse().code();
      if ("NoSuchKey".equals(codigo) || "NoSuchObject".equals(codigo)) {
        return false;
      }
      throw e;
    }
  }

  /** Solo descarga de objetos: no permite listar el bucket ni escribir. */
  private String politicaLecturaAnonima() {
    return """
        {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
        "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}"""
        .formatted(bucket);
  }
}
