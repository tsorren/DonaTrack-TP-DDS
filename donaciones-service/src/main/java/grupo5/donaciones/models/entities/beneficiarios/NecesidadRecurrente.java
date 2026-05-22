package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.donaciones.models.entities.bienes.SubCategoria;
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

  public NecesidadRecurrente(
      SubCategoria subcategoria,
      Integer cantidadNecesitada,
      String descripcion,
      Period periodo,
      LocalDate fechaInicioPeriodo) {

    super(subcategoria, cantidadNecesitada, descripcion);

    this.periodo = periodo;
    this.setFechaPeriodo(fechaInicioPeriodo);

    validarNecesidadRecurrente();
  }

  private void validarNecesidadRecurrente() {

    if (periodo == null) {
      throw new IllegalArgumentException("La necesidad recurrente debe tener un período definido.");
    }

    if (fechaInicioPeriodo == null) {
      throw new IllegalArgumentException("La fecha de inicio del período no puede ser nula.");
    }

    if (fechaInicioPeriodo.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("La fecha de inicio del período no puede ser futura.");
    }
  }

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

    if (fecha == null) {
      throw new IllegalArgumentException("La fecha del período no puede ser nula.");
    }

    this.fechaInicioPeriodo = fecha;
    this.fechaFinPeriodo = fechaInicioPeriodo.plus(this.periodo);
  }

  // Inicio <= fecha < fin
  // Dia 1 a 7: 8 sería el otro lunes y queda afuera
  public boolean estaEnPeriodo(LocalDate fecha) {

    if (fecha == null) {
      throw new IllegalArgumentException("La fecha del período no puede ser nula.");
    }
    return (fechaInicioPeriodo.isBefore(fecha) || fechaInicioPeriodo.isEqual(fecha))
        && fechaFinPeriodo.isAfter(fecha);
  }
}
