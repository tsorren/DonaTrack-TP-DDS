package grupo5.donaciones.models.ports;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Puerto de almacenamiento de objetos (padrones CSV y fotos de bienes). Expresado en bucket y
 * clave, sin tipos de ningún proveedor: el adapter de MinIO y el de memoria lo implementan por
 * igual.
 *
 * <p>Las claves las genera Donaciones (nunca el nombre del archivo del cliente) y todo adapter las
 * valida con {@link #validarClave}. Las fallas del almacenamiento se informan con {@link
 * AlmacenamientoException}.
 */
public interface StoragePort {

  /** Qué permite hacer una URL temporal sobre el objeto. */
  enum Operacion {
    SUBIR,
    DESCARGAR
  }

  /** Tamaño en bytes y tipo de contenido de un objeto guardado. */
  record Metadatos(long tamanio, String tipoContenido) {}

  /**
   * Guarda el contenido bajo {@code bucket/clave}. Si ya existe un objeto con esa clave, lo pisa.
   */
  void guardar(
      String bucket, String clave, InputStream contenido, long tamanio, String tipoContenido);

  /** Abre el objeto como stream. Quien llama es responsable de cerrarlo. */
  InputStream abrir(String bucket, String clave);

  /** Metadatos del objeto, o vacío si no existe. */
  Optional<Metadatos> metadatos(String bucket, String clave);

  /**
   * URL firmada que permite la {@code operacion} sobre {@code bucket/clave} durante {@code
   * vigencia}.
   */
  String urlTemporal(String bucket, String clave, Duration vigencia, Operacion operacion);

  /** Letras, números, guiones, guiones bajos y puntos, con a lo sumo una barra entre segmentos. */
  Pattern CLAVE_VALIDA =
      Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9._-]*(/[A-Za-z0-9_-][A-Za-z0-9._-]*)*");

  /**
   * Rechaza claves que podrían escapar del bucket o de su prefijo: vacías, absolutas, con {@code
   * ..}, barras invertidas o caracteres de control. Devuelve la misma clave si es válida.
   */
  static String validarClave(String clave) {
    if (clave == null || clave.length() > 512 || !CLAVE_VALIDA.matcher(clave).matches()) {
      throw new IllegalArgumentException("Clave de objeto inválida");
    }
    for (String segmento : clave.split("/")) {
      if (segmento.contains("..")) {
        throw new IllegalArgumentException("Clave de objeto inválida");
      }
    }
    return clave;
  }

  /** Falla del almacenamiento (objeto inexistente al abrirlo, proveedor caído). */
  class AlmacenamientoException extends InfrastructureException {
    public AlmacenamientoException(Throwable causa) {
      super(ErrorCatalog.ALMACENAMIENTO_ERROR, causa);
    }
  }
}
