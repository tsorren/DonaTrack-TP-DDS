package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.Bien;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DonacionAsignada {
  private Bien bien;
  private Integer cantidad;
  private LocalDate fechaAsignacion;

  public DonacionAsignada(
          Bien bien,
          Integer cantidad,
          LocalDate fechaAsignacion) {

    validarDonacionAsignada(bien, cantidad, fechaAsignacion);

    this.bien = bien;
    this.cantidad = cantidad;
    this.fechaAsignacion = fechaAsignacion;
  }

  public DonacionAsignada() {

  }

  private void validarDonacionAsignada(
          Bien bien,
          Integer cantidad,
          LocalDate fechaAsignacion){
    if(bien == null) {
      throw new IllegalArgumentException("La donación asignada debe tener un bien.");
    }

    if(cantidad == null || cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad asignada debe ser mayor a cero.");
    }

    if(fechaAsignacion != null && fechaAsignacion.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("La fecha de asignación no puede ser futura.");
    }
  }
}
