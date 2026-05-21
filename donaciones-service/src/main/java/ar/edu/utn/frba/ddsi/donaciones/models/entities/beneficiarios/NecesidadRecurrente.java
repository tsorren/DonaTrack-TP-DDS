package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import java.time.LocalDate;
import java.time.Period;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
  private Period periodo;
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
    this.fechaFinPeriodo = fechaInicioPeriodo.plus(this.periodo);
  }

  // Inicio <= fecha < fin
  // Dia 1 a 7: 8 sería el otro lunes y queda afuera
  public boolean estaEnPeriodo(LocalDate fecha) {
    return (fechaInicioPeriodo.isBefore(fecha) || fechaInicioPeriodo.isEqual(fecha))
        && fechaFinPeriodo.isAfter(fecha);
  }
}
