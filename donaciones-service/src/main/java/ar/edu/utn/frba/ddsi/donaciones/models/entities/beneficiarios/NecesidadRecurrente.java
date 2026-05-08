package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
  private Periodo periodo;
  private LocalDate fechaInicioPeriodo;
  private LocalDate fechaFinPeriodo;

  @Override
  public Integer cantidadAcumulada() {
    return this.donacionesAsignadas.stream()
        .filter(d -> this.estaEnPeriodo(d.getFechaAsignacion()))
        .mapToInt(DonacionAsignada::getCantidad)
        .sum();
  }

  // Esto se va a ejecutar con un planificador cada el tiempo indicado en Periodo
  public void reiniciarPeriodo() {
    this.setFechaPeriodo(LocalDate.now());
  }

  public void setFechaPeriodo(LocalDate fecha) {
    this.fechaInicioPeriodo = fecha;
    this.fechaFinPeriodo =
        switch (this.periodo) {
          case SEMANAL -> fechaInicioPeriodo.plusWeeks(1);
          case MENSUAL -> fechaInicioPeriodo.plusMonths(1);
          case ANUAL -> fechaInicioPeriodo.plusYears(1);
          case QUINCENAL -> fechaInicioPeriodo.plusDays(15);
        };
  }

  // Inicio <= fecha < fin
  // Dia 1 a 7: 8 sería el otro lunes y queda afuera
  public boolean estaEnPeriodo(LocalDate fecha) {
    return (fechaInicioPeriodo.isBefore(fecha) || fechaInicioPeriodo.isEqual(fecha))
        && fechaFinPeriodo.isAfter(fecha);
  }
}
