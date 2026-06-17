package grupo5.donaciones.models.entities.donaciones;

import grupo5.donaciones.models.entities.personas.Direccion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Deposito {
  private String nombre;
  private Direccion direccion;

  public Deposito(String nombre, Direccion direccion) {
    if (nombre == null || nombre.isBlank()) {
      throw new IllegalArgumentException("El depósito debe tener un nombre.");
    }
    if (direccion == null) {
      throw new IllegalArgumentException("El depósito debe tener una dirección.");
    }
    this.nombre = nombre;
    this.direccion = direccion;
  }
}
