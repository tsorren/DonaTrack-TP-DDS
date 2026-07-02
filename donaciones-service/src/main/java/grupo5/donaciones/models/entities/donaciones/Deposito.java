package grupo5.donaciones.models.entities.donaciones;

import grupo5.donaciones.models.entities.ubicaciones.Direccion;

public record Deposito(String nombre, Direccion direccion) {
  public Deposito {
    if (nombre == null || nombre.isBlank()) {
      throw new IllegalArgumentException("El depósito debe tener un nombre.");
    }
    if (direccion == null) {
      throw new IllegalArgumentException("El depósito debe tener una dirección.");
    }
  }
}
