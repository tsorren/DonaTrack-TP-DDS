package grupo5.cliente.vistas;

import java.util.List;
import java.util.Map;

/**
 * Seguimiento del donante (RF-09): sus donaciones (la pantalla separa las que van en curso y las
 * que esperan destino), el lugar en el mapa de las que tienen uno, por clave de donación, y cuándo
 * se leyeron las posiciones ("hoy, 15 de octubre, a las 16:25").
 */
public record DonanteSeguimientoVista(
    List<DonanteDonacionVista> donaciones, Map<String, Punto> puntos, String actualizado) {

  /** La posición aproximada del camión y la de la entidad que la recibe. */
  public record Punto(Double lat, Double lng, Double destinoLat, Double destinoLng) {}
}
