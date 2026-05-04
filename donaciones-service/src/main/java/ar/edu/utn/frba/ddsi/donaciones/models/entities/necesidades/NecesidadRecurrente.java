package ar.edu.utn.frba.ddsi.donaciones.models.entities.necesidades;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
  private int cantidadObjetivo;
  private int cantidadAcumulada;
  private Periodo periodo;
  private LocalDate fechaInicioPeriodo;

  @Override
  public boolean estaSatisfecha() {
    if (periodoVencido()) {
      reiniciarPeriodo();
    }
    return cantidadAcumulada >= cantidadObjetivo;
  }

  private void reiniciarPeriodo() {
    this.cantidadAcumulada = 0;
    this.fechaInicioPeriodo = LocalDate.now();
  }

  private boolean periodoVencido() {
    LocalDate ahora = LocalDate.now();

    switch (this.periodo) {
      case SEMANAL:
        return fechaInicioPeriodo.plusWeeks(1).isBefore(ahora);
      case MENSUAL:
        return fechaInicioPeriodo.plusMonths(1).isBefore(ahora);
      case ANUAL:
        return fechaInicioPeriodo.plusYears(1).isBefore(ahora);
      case QUINCENAL:
        return fechaInicioPeriodo.plusDays(15).isBefore(ahora);
      default:
        return false;
    }
  }
}
