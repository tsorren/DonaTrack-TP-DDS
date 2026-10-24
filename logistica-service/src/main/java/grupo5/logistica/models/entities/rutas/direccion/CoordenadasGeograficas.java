package grupo5.logistica.models.entities.rutas.direccion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.List;

/**
 * Value Object que representa coordenadas geográficas (WGS84) de un destino de entrega.
 *
 * <p>En MongoDB (persistencia definitiva) se mapeará a un objeto GeoJSON {@code Point} cuyo arreglo
 * {@code coordinates} utiliza el orden {@code [longitud, latitud]} provisto por {@link
 * #toGeoJsonCoordinates()}.
 */
public record CoordenadasGeograficas(double latitud, double longitud) {

  private static final double MIN_LATITUD = -90.0;
  private static final double MAX_LATITUD = 90.0;
  private static final double MIN_LONGITUD = -180.0;
  private static final double MAX_LONGITUD = 180.0;

  public CoordenadasGeograficas {
    if (Double.isNaN(latitud)
        || Double.isInfinite(latitud)
        || latitud < MIN_LATITUD
        || latitud > MAX_LATITUD) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (Double.isNaN(longitud)
        || Double.isInfinite(longitud)
        || longitud < MIN_LONGITUD
        || longitud > MAX_LONGITUD) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  /**
   * Retorna las coordenadas en el orden exigido por el estándar GeoJSON {@code Point}: {@code
   * [longitud, latitud]}.
   */
  public List<Double> toGeoJsonCoordinates() {
    return List.of(longitud, latitud);
  }
}
