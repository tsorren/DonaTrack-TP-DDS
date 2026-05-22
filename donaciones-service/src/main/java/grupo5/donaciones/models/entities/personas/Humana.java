package grupo5.donaciones.models.entities.personas;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Humana extends Persona {
  private String nombre;
  private String apellido;
  private Genero genero;
  private LocalDate fechaNacimiento;

  public Humana(String nombre, String apellido, LocalDate fechaNacimiento) {
    validarDatosHumanos(nombre, apellido, fechaNacimiento);

    this.nombre = nombre;
    this.apellido = apellido;
    this.fechaNacimiento = fechaNacimiento;
  }

  private void validarDatosHumanos(String nombre, String apellido, LocalDate fechaNacimiento) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new IllegalArgumentException("El nombre de la persona no puede estar vacío.");
    }
    if (apellido == null || apellido.trim().isEmpty()) {
      throw new IllegalArgumentException("El apellido de la persona no puede estar vacío.");
    }
    if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
    }
  }

  @Override
  public void anonimizar() {
    // El documento o DNI se ofusca o limpia

    this.nombre = "ANONIMIZADO";
    this.apellido = "ANONIMIZADO";
    this.genero = null;
    this.fechaNacimiento = null;
    this.setDocumento(null);
    if (this.getDireccion() != null) {
      this.getDireccion().anonimizar();
    }
  }
}
