package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.ZoneId;

public record Bien(String descripcion, String fotoUrl, LocalDate fechaVencimiento, Estado estado) {
  public Bien {
    validarReglasDeNegocio(descripcion);
  }

  private static void validarReglasDeNegocio(String descripcion) {
    // 1. Validar que la descripción no sea vacía
    if (descripcion == null || descripcion.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DESCRIPCION_BIEN_VACIA);
    }
  }

  // metodos
  public boolean estaVencido() {
    if (this.fechaVencimiento == null) return false;
    return this.fechaVencimiento.isBefore(LocalDate.now(ZoneId.systemDefault()));
  }
}
