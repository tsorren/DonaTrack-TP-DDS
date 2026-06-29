package grupo5.logistica.models.entities.rutas.direccion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;

public record Direccion(
    String calle,
    Integer altura,
    Integer piso,
    String departamento,
    String codigoPostal,
    Localidad localidad) {

  public Direccion {
    validarDireccion(calle, altura, codigoPostal, localidad);
  }

  private static void validarDireccion(
      String calle, Integer altura, String codigoPostal, Localidad localidad) {
    if (calle == null || calle.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DIRECCION_CALLE_VACIA);
    }
    if (altura == null || altura <= 0) {
      throw new ValidationException(ErrorCatalog.DIRECCION_ALTURA_INVALIDA);
    }
    if (codigoPostal == null || codigoPostal.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DIRECCION_CODIGO_POSTAL_VACIO);
    }
    if (localidad == null) {
      throw new ValidationException(ErrorCatalog.DIRECCION_LOCALIDAD_NULA);
    }
  }
}
