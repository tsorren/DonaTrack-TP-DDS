package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionAsignada {
  private DonacionIndependiente donacionIndependiente;
  private Necesidad necesidad;
  private LocalDateTime fechaAsignacion;

  public DonacionAsignada(
      DonacionIndependiente donacionIndependiente, LocalDateTime fechaAsignacion) {
    this.donacionIndependiente = donacionIndependiente;
    this.fechaAsignacion = fechaAsignacion;

    validarDonacionAsignada();
  }

  private void validarDonacionAsignada() {
    if (this.donacionIndependiente == null) {
      throw new IllegalArgumentException(
          "La donación asignada debe tener una donación independiente.");
    }

    if (this.fechaAsignacion != null && this.fechaAsignacion.isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException("La fecha de asignación no puede ser futura.");
    }
  }

  public Integer getCantidad() {
    return this.donacionIndependiente.getCantidad();
  }
}
