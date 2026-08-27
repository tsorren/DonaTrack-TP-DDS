package grupo5.incentivos.models.entities.insignias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;

public record Insignia(String nombre, String descripcion, String imagenUrl) {

  public Insignia {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_SIN_NOMBRE);
    }
  }
}
