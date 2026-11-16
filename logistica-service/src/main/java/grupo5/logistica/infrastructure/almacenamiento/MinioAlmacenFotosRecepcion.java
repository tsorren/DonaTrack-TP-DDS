package grupo5.logistica.infrastructure.almacenamiento;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import grupo5.logistica.services.AlmacenFotosRecepcion;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;

/**
 * {@link AlmacenFotosRecepcion} sobre MinIO, en el bucket privado {@value #BUCKET}. Usa dos
 * clientes con las mismas credenciales, como Donaciones: {@code cliente} habla con MinIO por la red
 * interna y {@code clienteFirmas} solo firma URLs con el host que ve el navegador (la firma incluye
 * el host, y firmar no usa la red).
 */
public class MinioAlmacenFotosRecepcion implements AlmacenFotosRecepcion {

  public static final String BUCKET = "logistica-fotos-recepcion";

  private final MinioClient cliente;
  private final MinioClient clienteFirmas;

  public MinioAlmacenFotosRecepcion(MinioClient cliente, MinioClient clienteFirmas) {
    this.cliente = cliente;
    this.clienteFirmas = clienteFirmas;
  }

  /** Crea el bucket si no existe. Idempotente; lo deja privado (sin política de lectura). */
  public void crearBucketSiFalta() {
    try {
      if (cliente.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
        return;
      }
      cliente.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    } catch (ErrorResponseException e) {
      // Otra réplica lo creó entre el chequeo y la creación.
      if (!"BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
        throw new InfrastructureException(ErrorCatalog.ALMACENAMIENTO_ERROR, e);
      }
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new InfrastructureException(ErrorCatalog.ALMACENAMIENTO_ERROR, e);
    }
  }

  @Override
  public void guardar(String clave, byte[] contenido, String tipoContenido) {
    try {
      cliente.putObject(
          PutObjectArgs.builder().bucket(BUCKET).object(clave).stream(
                  new ByteArrayInputStream(contenido), contenido.length, -1)
              .contentType(tipoContenido)
              .build());
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new InfrastructureException(ErrorCatalog.ALMACENAMIENTO_ERROR, e);
    }
  }

  @Override
  public boolean existe(String clave) {
    try {
      cliente.statObject(StatObjectArgs.builder().bucket(BUCKET).object(clave).build());
      return true;
    } catch (ErrorResponseException e) {
      String codigo = e.errorResponse().code();
      if ("NoSuchKey".equals(codigo) || "NoSuchObject".equals(codigo)) {
        return false;
      }
      throw new InfrastructureException(ErrorCatalog.ALMACENAMIENTO_ERROR, e);
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new InfrastructureException(ErrorCatalog.ALMACENAMIENTO_ERROR, e);
    }
  }

  @Override
  public String urlFirmada(String clave, Duration vigencia) {
    try {
      return clienteFirmas.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(BUCKET)
              .object(clave)
              .expiry(Math.toIntExact(vigencia.toSeconds()))
              .build());
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new InfrastructureException(ErrorCatalog.ALMACENAMIENTO_ERROR, e);
    }
  }
}
