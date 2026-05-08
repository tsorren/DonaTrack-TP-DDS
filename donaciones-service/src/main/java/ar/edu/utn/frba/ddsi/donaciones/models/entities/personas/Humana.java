package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Humana extends Persona {
  private String nombre;
  private String apellido;
  private Genero genero;
  private LocalDate fechaNacimiento;
}
