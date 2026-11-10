package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.Optional;

/**
 * Puerto de salida hacia la fuente de la posición del camión (ADR-E6-07, D23). Hoy la implementa
 * una simulación; un GPS real o un callback del proveedor externo se suman como otro adaptador.
 */
public interface FuentePosicionCamion {

  /** Posición actual del camión de una ruta en traslado, o vacío si la fuente no la conoce. */
  Optional<PosicionCamion> posicionActual(Ruta ruta);
}
