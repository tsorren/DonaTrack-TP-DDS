package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bien {
  private String descripcion;
  private String fotoUrl;
  private LocalDate fechaVencimiento;
  private Estado estado;

  public Bien(String descripcion, String fotoUrl, LocalDate fechaVencimiento, Estado estado) {
    this.descripcion = descripcion;
    this.fotoUrl = fotoUrl;
    this.fechaVencimiento = fechaVencimiento;
    this.estado = estado;

    validarReglasDeNegocio();
  }

  private void validarReglasDeNegocio() {
    // 1. Validar que la descripción no sea vacía
    if (this.descripcion == null || this.descripcion.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DESCRIPCION_BIEN_VACIA);
    }
  }

  // metodos
  public boolean estaVencido() {
    if (this.fechaVencimiento == null) return false;
    return this.fechaVencimiento.isBefore(LocalDate.now(ZoneId.systemDefault()));
  }
}
