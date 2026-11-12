package grupo5.donaciones.dto.propuestas;

/**
 * Cuántas propuestas pendientes hay y en qué situación (R11-24): {@code pendientes = libres +
 * enConflicto + sinStock}. Desde R11-35 (aditivo), {@code decididasSinFecha}: cuántas aprobadas o
 * descartadas no tienen fecha de decisión guardada (las de antes de V22), que no entran en un
 * período. Son conteos de la base.
 */
public record ResumenPropuestasDTO(
    long pendientes, long libres, long enConflicto, long sinStock, long decididasSinFecha) {

  /** Sin el conteo de R11-35. */
  public ResumenPropuestasDTO(long pendientes, long libres, long enConflicto, long sinStock) {
    this(pendientes, libres, enConflicto, sinStock, 0);
  }
}
