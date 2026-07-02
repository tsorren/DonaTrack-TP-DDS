package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.ZoneId;

public record Bien(
    String descripcion,
    String fotoUrl,
    LocalDate fechaVencimiento,
    Estado estado,
    Double pesoUnitario,
    Double volumenUnitario) {
  public Bien {
    validarReglasDeNegocio(descripcion, pesoUnitario, volumenUnitario);
  }

  private static void validarReglasDeNegocio(
      String descripcion, Double pesoUnitario, Double volumenUnitario) {
    // 1. Validar que la descripción no sea vacía
    if (descripcion == null || descripcion.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DESCRIPCION_BIEN_VACIA);
    }

    if (pesoUnitario == null || pesoUnitario <= 0) {
      throw new ValidationException(ErrorCatalog.DIMENSIONES_BIEN_INVALIDAS);
    }

    if (volumenUnitario == null || volumenUnitario <= 0) {
      throw new ValidationException(ErrorCatalog.DIMENSIONES_BIEN_INVALIDAS);
    }
  }

  // metodos
  public boolean estaVencido() {
    if (this.fechaVencimiento == null) return false;
    return this.fechaVencimiento.isBefore(LocalDate.now(ZoneId.systemDefault()));
  }
}
