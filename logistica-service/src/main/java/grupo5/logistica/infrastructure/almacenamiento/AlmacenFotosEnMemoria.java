package grupo5.logistica.infrastructure.almacenamiento;

import grupo5.logistica.services.AlmacenFotosRecepcion;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AlmacenFotosRecepcion} en memoria, para el modo sin MinIO (perfil en memoria y tests). La
 * "URL firmada" no se puede abrir desde un navegador: solo identifica el objeto.
 */
public class AlmacenFotosEnMemoria implements AlmacenFotosRecepcion {

  private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();

  @Override
  public void guardar(String clave, byte[] contenido, String tipoContenido) {
    objetos.put(clave, contenido.clone());
  }

  @Override
  public boolean existe(String clave) {
    return objetos.containsKey(clave);
  }

  @Override
  public String urlFirmada(String clave, Duration vigencia) {
    return "memoria://" + MinioAlmacenFotosRecepcion.BUCKET + "/" + clave;
  }

  /** Contenido guardado bajo la clave, o {@code null}. Para tests. */
  public byte[] contenido(String clave) {
    byte[] contenido = objetos.get(clave);
    return contenido == null ? null : contenido.clone();
  }
}
