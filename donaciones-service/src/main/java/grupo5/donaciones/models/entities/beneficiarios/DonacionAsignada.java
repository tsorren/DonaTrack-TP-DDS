package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
      throw new ValidationException(ErrorCatalog.DONACION_ASIGNADA_SIN_DONACION_INDEPENDIENTE);
    }

    if (this.fechaAsignacion != null
        && this.fechaAsignacion.isAfter(LocalDateTime.now(ZoneId.systemDefault()))) {
      throw new ValidationException(ErrorCatalog.FECHA_ASIGNACION_FUTURA);
    }
  }

  public Integer getCantidad() {
    return this.donacionIndependiente.getCantidad();
  }
}
