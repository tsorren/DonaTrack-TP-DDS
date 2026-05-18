package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.MedioDeContacto;
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

  public Humana(
      String nombre, String apellido, LocalDate fechaNacimiento, MedioDeContacto contactoInicial) {
    super(contactoInicial);
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
}
