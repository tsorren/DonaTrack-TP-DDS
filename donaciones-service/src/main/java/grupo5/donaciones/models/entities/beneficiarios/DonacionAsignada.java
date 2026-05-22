package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.donaciones.models.entities.bienes.Bien;
import java.time.LocalDate;

@Getter
@Setter
public class DonacionAsignada {
  private Bien bien;
  private Integer cantidad;
  private LocalDate fechaAsignacion;

  public DonacionAsignada(Bien bien, Integer cantidad, LocalDate fechaAsignacion) {
    this.bien = bien;
    this.cantidad = cantidad;
    this.fechaAsignacion = fechaAsignacion;

    validarDonacionAsignada();
  }

  private void validarDonacionAsignada() {
    if (this.bien == null) {
      throw new IllegalArgumentException("La donación asignada debe tener un bien.");
    }

    if (this.cantidad == null || this.cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad asignada debe ser mayor a cero.");
    }

    if (this.fechaAsignacion != null && this.fechaAsignacion.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("La fecha de asignación no puede ser futura.");
    }
  }
}
