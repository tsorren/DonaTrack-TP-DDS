package grupo5.donaciones.models.entities.necesidades;

import java.time.Period;
import java.util.Arrays;

/**
 * Los tres largos de período de una necesidad recurrente (ADR 20261102 del ciclo de vida, punto 4).
 * El período empieza el día de la publicación, así que la quincena es de 15 días y no del
 * calendario.
 */
public enum Periodicidad {
  SEMANAL(Period.ofDays(7)),
  QUINCENAL(Period.ofDays(15)),
  MENSUAL(Period.ofMonths(1));

  private final Period periodo;

  Periodicidad(Period periodo) {
    this.periodo = periodo;
  }

  public Period periodo() {
    return periodo;
  }

  /**
   * La periodicidad de un {@link Period} guardado, o {@code null} si es de otro largo: las
   * recurrentes creadas antes de cerrar los largos conservan el suyo.
   */
  public static Periodicidad de(Period periodo) {
    return Arrays.stream(values()).filter(p -> p.periodo.equals(periodo)).findFirst().orElse(null);
  }
}
