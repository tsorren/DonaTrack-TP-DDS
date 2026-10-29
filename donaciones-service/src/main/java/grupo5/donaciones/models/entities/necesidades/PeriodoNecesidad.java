package grupo5.donaciones.models.entities.necesidades;

import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Un período de una necesidad recurrente. No conoce sus donaciones (DTI-06): cada donación
 * independiente guarda el {@code id} del período al que se asignó, y lo acumulado se calcula afuera
 * (suma de esas donaciones) y se pasa como parámetro.
 */
public record PeriodoNecesidad(UUID id, LocalDate fechaFin, Integer cantidadObjetivo) {

  private static final Logger logger = Logger.getLogger(PeriodoNecesidad.class.getName());

  public PeriodoNecesidad(LocalDate fechaFin, Integer cantidadObjetivo) {
    this(UUID.randomUUID(), fechaFin, cantidadObjetivo);
  }

  public boolean estaSatisfechaCon(int cantidadAcumulada) {
    return cantidadAcumulada >= this.cantidadObjetivo;
  }

  public boolean estaEnPeriodo(LocalDate fecha) {
    if (this.fechaFin == null) {
      return true;
    }
    return !fecha.isAfter(this.fechaFin);
  }

  public void finalizo(int cantidadAcumulada) {
    if (!this.estaSatisfechaCon(cantidadAcumulada)) {
      String msj = "El período cerró sin alcanzar la meta de " + this.cantidadObjetivo;
      logger.warning(msj);
    }
  }
}
