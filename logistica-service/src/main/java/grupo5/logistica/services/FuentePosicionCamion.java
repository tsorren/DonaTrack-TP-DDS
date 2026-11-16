package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida hacia la fuente de la posición del camión (ADR-E6-07, D23). Hoy la implementa
 * una simulación; un GPS real o un callback del proveedor externo se suman como otro adaptador.
 */
public interface FuentePosicionCamion {

  /** Posición actual del camión de una ruta en traslado, o vacío si la fuente no la conoce. */
  Optional<PosicionCamion> posicionActual(Ruta ruta);

  /**
   * Entregas de la ruta a cuya puerta ya llegó el camión (HB-22), o vacío si la fuente no lo sabe.
   * Se pasan a {@code EN_PUERTA} las que sigan en traslado.
   */
  default List<UUID> entregasAlcanzadas(Ruta ruta) {
    return List.of();
  }
}
