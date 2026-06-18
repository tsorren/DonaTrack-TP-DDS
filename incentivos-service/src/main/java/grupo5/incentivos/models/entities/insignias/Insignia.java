package grupo5.incentivos.models.entities.insignias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZoneId;

@Getter
@Setter
public class Insignia {

  private String nombre;
  private String descripcion;
  private String imagenUrl;
  private boolean visible;
  private LocalDate fechaObtenida;

  public Insignia(String nombre, String descripcion, String imagenUrl) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_SIN_NOMBRE);
    }
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.imagenUrl = imagenUrl;
    this.visible = true;
    this.fechaObtenida = LocalDate.now(ZoneId.systemDefault());
  }
}
