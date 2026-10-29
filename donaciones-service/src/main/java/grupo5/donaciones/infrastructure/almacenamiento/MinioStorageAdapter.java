package grupo5.donaciones.infrastructure.almacenamiento;

import grupo5.donaciones.models.ports.StoragePort;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Optional;

/**
 * {@link StoragePort} sobre MinIO (API S3). Usa dos clientes con las mismas credenciales: {@code
 * cliente} habla con MinIO por la red interna y {@code clienteFirmas} solo firma URLs con el host
 * que ve el navegador (la firma incluye el host, y firmar no usa la red).
 */
public class MinioStorageAdapter implements StoragePort {

  private final MinioClient cliente;
  private final MinioClient clienteFirmas;

  public MinioStorageAdapter(MinioClient cliente, MinioClient clienteFirmas) {
    this.cliente = cliente;
    this.clienteFirmas = clienteFirmas;
  }

  @Override
  public void guardar(
      String bucket, String clave, InputStream contenido, long tamanio, String tipoContenido) {
    StoragePort.validarClave(clave);
    try {
      cliente.putObject(
          PutObjectArgs.builder().bucket(bucket).object(clave).stream(contenido, tamanio, -1)
              .contentType(tipoContenido)
              .build());
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new AlmacenamientoException(e);
    }
  }

  @Override
  public InputStream abrir(String bucket, String clave) {
    StoragePort.validarClave(clave);
    try {
      return cliente.getObject(GetObjectArgs.builder().bucket(bucket).object(clave).build());
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new AlmacenamientoException(e);
    }
  }

  @Override
  public Optional<Metadatos> metadatos(String bucket, String clave) {
    StoragePort.validarClave(clave);
    try {
      StatObjectResponse stat =
          cliente.statObject(StatObjectArgs.builder().bucket(bucket).object(clave).build());
      return Optional.of(new Metadatos(stat.size(), stat.contentType()));
    } catch (ErrorResponseException e) {
      String codigo = e.errorResponse().code();
      if ("NoSuchKey".equals(codigo) || "NoSuchObject".equals(codigo)) {
        return Optional.empty();
      }
      throw new AlmacenamientoException(e);
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new AlmacenamientoException(e);
    }
  }

  @Override
  public String urlTemporal(String bucket, String clave, Duration vigencia, Operacion operacion) {
    StoragePort.validarClave(clave);
    Method metodo = operacion == Operacion.SUBIR ? Method.PUT : Method.GET;
    try {
      return clienteFirmas.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(metodo)
              .bucket(bucket)
              .object(clave)
              .expiry(Math.toIntExact(vigencia.toSeconds()))
              .build());
    } catch (MinioException | IOException | GeneralSecurityException e) {
      throw new AlmacenamientoException(e);
    }
  }
}
