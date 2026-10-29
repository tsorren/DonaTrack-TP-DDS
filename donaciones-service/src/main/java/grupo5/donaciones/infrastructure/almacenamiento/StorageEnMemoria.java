package grupo5.donaciones.infrastructure.almacenamiento;

import grupo5.donaciones.models.ports.StoragePort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link StoragePort} en memoria, para el perfil por defecto y los tests (simetría con los
 * repositorios en memoria). Las URLs temporales usan el esquema {@code memoria://}: sirven para
 * identificar el objeto, no para subir ni descargar por HTTP.
 */
public class StorageEnMemoria implements StoragePort {

  static final String ESQUEMA_URL = "memoria://";

  // Clase y no record: un record con un arreglo compararía el arreglo por referencia (S6218)
  private static final class Objeto {
    private final byte[] contenido;
    private final String tipoContenido;

    Objeto(byte[] contenido, String tipoContenido) {
      this.contenido = contenido;
      this.tipoContenido = tipoContenido;
    }

    byte[] contenido() {
      return contenido;
    }

    String tipoContenido() {
      return tipoContenido;
    }
  }

  private final Map<String, Objeto> objetos = new ConcurrentHashMap<>();

  @Override
  public void guardar(
      String bucket, String clave, InputStream contenido, long tamanio, String tipoContenido) {
    StoragePort.validarClave(clave);
    try {
      objetos.put(ruta(bucket, clave), new Objeto(contenido.readAllBytes(), tipoContenido));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public InputStream abrir(String bucket, String clave) {
    StoragePort.validarClave(clave);
    Objeto objeto = objetos.get(ruta(bucket, clave));
    if (objeto == null) {
      throw new AlmacenamientoException(
          new IllegalStateException("No existe el objeto " + ruta(bucket, clave)));
    }
    return new ByteArrayInputStream(objeto.contenido());
  }

  @Override
  public Optional<Metadatos> metadatos(String bucket, String clave) {
    StoragePort.validarClave(clave);
    return Optional.ofNullable(objetos.get(ruta(bucket, clave)))
        .map(objeto -> new Metadatos(objeto.contenido().length, objeto.tipoContenido()));
  }

  @Override
  public String urlTemporal(String bucket, String clave, Duration vigencia, Operacion operacion) {
    StoragePort.validarClave(clave);
    return ESQUEMA_URL + ruta(bucket, clave);
  }

  private static String ruta(String bucket, String clave) {
    return bucket + "/" + clave;
  }
}
