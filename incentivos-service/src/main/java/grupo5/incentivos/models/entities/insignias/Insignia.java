package grupo5.incentivos.models.entities.insignias;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

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
      throw new IllegalArgumentException("La insignia debe tener un nombre");
    }
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.imagenUrl = imagenUrl;
    this.visible = true;
    this.fechaObtenida = LocalDate.now();
  }
}
