package grupo5.logistica.models.entities.rutas;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Hora estimada de llegada de una entrega en camino (HG-02, spec
 * docs/specs/active/HG-02-hora-estimada-de-llegada-spec.md).
 *
 * <p>Recorre en línea recta los tramos desde la última posición del camión a una velocidad media
 * urbana y suma una demora fija por cada parada pendiente anterior. Se ancla en la hora de la
 * posición, no en la de la consulta. La velocidad y la demora son valores de diseño, no medidos.
 */
public final class EstimadorDeLlegada {

  private static final double VELOCIDAD_MEDIA_KM_H = 20;
  private static final Duration DEMORA_POR_PARADA = Duration.ofMinutes(10);
  private static final double RADIO_TIERRA_KM = 6371;
  private static final double SEGUNDOS_POR_HORA = 3600;

  private EstimadorDeLlegada() {}

  /**
   * La hora a la que se estima llegar al último punto de {@code tramos}, pasando por los anteriores
   * en orden, desde {@code origen}, con {@code paradasPrevias} paradas antes del destino.
   */
  public static Instant estimar(
      PosicionCamion origen, List<CoordenadasGeograficas> tramos, int paradasPrevias) {
    double km = 0;
    double latitud = origen.latitud();
    double longitud = origen.longitud();
    for (CoordenadasGeograficas punto : tramos) {
      km += distanciaKm(latitud, longitud, punto.latitud(), punto.longitud());
      latitud = punto.latitud();
      longitud = punto.longitud();
    }
    long segundosDeViaje = Math.round(km / VELOCIDAD_MEDIA_KM_H * SEGUNDOS_POR_HORA);
    return origen
        .registradaEn()
        .plusSeconds(segundosDeViaje)
        .plus(DEMORA_POR_PARADA.multipliedBy(paradasPrevias));
  }

  /** Distancia por la fórmula del semiverseno (haversine). */
  private static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
    return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
  }
}
