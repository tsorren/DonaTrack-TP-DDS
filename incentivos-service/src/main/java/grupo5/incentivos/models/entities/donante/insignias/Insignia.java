package grupo5.incentivos.models.entities.donante.insignias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;

public record Insignia(
    String nombre, String descripcion, String imagenUrl, boolean visible, LocalDate fechaObtenida) {

  public Insignia {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_SIN_NOMBRE);
    }
  }

  public Insignia(String nombre, String descripcion, String imagenUrl) {
    this(nombre, descripcion, imagenUrl, true, null);
  }
}
