package grupo5.donaciones.models.entities.necesidades;

/** Resultado de un período de una necesidad recurrente, según lo recibido contra su objetivo. */
public enum ResultadoPeriodo {
  /** El período en curso, que todavía no llegó a su objetivo. */
  EN_CURSO,
  /** Lo recibido llegó al objetivo (también el período en curso, apenas llega). */
  CUMPLIDO,
  /** El período cerró sin llegar al objetivo. */
  INCOMPLETO
}
