package grupo5.logistica.services;

import java.time.Duration;

/**
 * Puerto del almacenamiento de las fotos de recepción (ADR-E6-08). Trabaja con claves de objeto,
 * sin tipos de ningún proveedor: el adaptador de MinIO y el de memoria lo implementan por igual. El
 * bucket lo fija cada adaptador.
 */
public interface AlmacenFotosRecepcion {

  /** Guarda el contenido bajo la clave. Si ya existe un objeto con esa clave, lo pisa. */
  void guardar(String clave, byte[] contenido, String tipoContenido);

  /** Si existe un objeto con esa clave. */
  boolean existe(String clave);

  /** URL firmada de lectura ({@code GET}) del objeto, válida durante {@code vigencia}. */
  String urlFirmada(String clave, Duration vigencia);
}
