package grupo5.logistica.models.entities.camiones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import java.time.Instant;
import java.util.Objects;

/**
 * Value Object con la última posición conocida de un camión (ADR-E6-07). {@code registradaEn} es la
 * hora de origen del dato, no la hora en que se guardó.
 */
public record PosicionCamion(double latitud, double longitud, Instant registradaEn) {

  public PosicionCamion {
    CoordenadasGeograficas.validar(latitud, longitud);
    if (Objects.isNull(registradaEn)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }

  public boolean esPosteriorA(PosicionCamion otra) {
    return otra == null || registradaEn.isAfter(otra.registradaEn);
  }
}
