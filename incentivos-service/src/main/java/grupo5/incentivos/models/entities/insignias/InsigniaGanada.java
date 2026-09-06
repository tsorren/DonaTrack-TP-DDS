package grupo5.incentivos.models.entities.insignias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;

public record InsigniaGanada(
    String nombre, String descripcion, String imagenUrl, boolean visible, LocalDate fechaObtenida) {

  public InsigniaGanada {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_SIN_NOMBRE);
    }
  }

  public InsigniaGanada conVisibilidad(boolean visible) {
    return new InsigniaGanada(nombre, descripcion, imagenUrl, visible, fechaObtenida);
  }
}
