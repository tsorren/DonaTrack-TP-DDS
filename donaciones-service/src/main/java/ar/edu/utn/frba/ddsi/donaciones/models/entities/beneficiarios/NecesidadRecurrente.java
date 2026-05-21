package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import java.time.LocalDate;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.SubCategoria;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
  private Periodo periodo;
  private LocalDate fechaInicioPeriodo;
  private LocalDate fechaFinPeriodo;

    public NecesidadRecurrente(
            SubCategoria subcategoria,
            Integer cantidadNecesitada,
            String descripcion,
            Periodo periodo,
            LocalDate fechaInicioPeriodo) {

        super(subcategoria, cantidadNecesitada, descripcion);

        validarNecesidadRecurrente(periodo, fechaInicioPeriodo);

        this.periodo = periodo;

        this.setFechaPeriodo(fechaInicioPeriodo);
    }
// constructor vacio para compatibilidad con test, borrar cuando se adapten los test al constructor validado
    public NecesidadRecurrente() {

    }

    private void validarNecesidadRecurrente(
            Periodo periodo,
            LocalDate fechaInicioPeriodo) {

        if (periodo == null) {
            throw new IllegalArgumentException(
                    "La necesidad recurrente debe tener un período definido.");
        }

        if (fechaInicioPeriodo == null) {
            throw new IllegalArgumentException(
                    "La fecha de inicio del período no puede ser nula.");
        }

        if (fechaInicioPeriodo.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de inicio del período no puede ser futura.");
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
        throw new IllegalArgumentException(
          "La fecha del período no puede ser nula.");
      }

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

    if (fecha == null) {
      throw new IllegalArgumentException(
          "La fecha del período no puede ser nula.");
    }
    return (fechaInicioPeriodo.isBefore(fecha) || fechaInicioPeriodo.isEqual(fecha))
        && fechaFinPeriodo.isAfter(fecha);
  }
}
