package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;

public record Deposito(String nombre, Direccion direccion) {
  public Deposito {
    if (nombre == null || nombre.isBlank()) {
      throw new ValidationException(ErrorCatalog.DEPOSITO_NOMBRE_NULO);
    }
    if (direccion == null) {
      throw new ValidationException(ErrorCatalog.DEPOSITO_DIRECCION_NULA);
    }
  }
}
