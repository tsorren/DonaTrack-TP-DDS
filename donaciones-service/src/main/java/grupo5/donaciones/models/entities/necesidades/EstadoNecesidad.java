package grupo5.donaciones.models.entities.necesidades;

/**
 * Estado de una necesidad, calculado en cada lectura (ADR 20261102 del ciclo de vida, punto 1). Una
 * recurrente solo está {@link #VIGENTE} o {@link #DADA_DE_BAJA}: cada período tiene su resultado.
 */
public enum EstadoNecesidad {
  /** Activa y, si es de una sola vez, todavía sin cumplir ni vencer. */
  VIGENTE,
  /** De una sola vez: lo recibido llegó a la cantidad. */
  CUMPLIDA,
  /** De una sola vez: pasó la fecha límite sin cumplirse. */
  TERMINADA,
  /** La entidad la dio de baja. */
  DADA_DE_BAJA
}
