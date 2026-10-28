package grupo5.donaciones.models.ports;

import java.io.InputStream;
import java.time.Duration;

/**
 * Puerto de almacenamiento de objetos (padrones CSV y fotos de bienes). Expresado en bucket y
 * clave, sin tipos de ningún proveedor: el adapter de MinIO y el de memoria lo implementan por
 * igual.
 *
 * <p>Las claves las genera Donaciones (nunca el nombre del archivo del cliente). Las fallas del
 * almacenamiento se informan con {@link AlmacenamientoException}.
 */
public interface StoragePort {

  /** Qué permite hacer una URL temporal sobre el objeto. */
  enum Operacion {
    SUBIR,
    DESCARGAR
  }

  /**
   * Guarda el contenido bajo {@code bucket/clave}. Si ya existe un objeto con esa clave, lo pisa.
   */
  void guardar(
      String bucket, String clave, InputStream contenido, long tamanio, String tipoContenido);

  /** Abre el objeto como stream. Quien llama es responsable de cerrarlo. */
  InputStream abrir(String bucket, String clave);

  /**
   * URL firmada que permite la {@code operacion} sobre {@code bucket/clave} durante {@code
   * vigencia}.
   */
  String urlTemporal(String bucket, String clave, Duration vigencia, Operacion operacion);

  /** Falla del almacenamiento (objeto inexistente, proveedor caído, clave inválida). */
  class AlmacenamientoException extends RuntimeException {
    public AlmacenamientoException(String mensaje) {
      super(mensaje);
    }

    public AlmacenamientoException(String mensaje, Throwable causa) {
      super(mensaje, causa);
    }
  }
}
