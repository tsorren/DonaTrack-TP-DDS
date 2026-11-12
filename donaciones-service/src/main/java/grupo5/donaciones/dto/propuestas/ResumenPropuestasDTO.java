package grupo5.donaciones.dto.propuestas;

/**
 * Cuántas propuestas pendientes hay y en qué situación (R11-24): {@code pendientes = libres +
 * enConflicto + sinStock}. Son conteos de la base.
 */
public record ResumenPropuestasDTO(long pendientes, long libres, long enConflicto, long sinStock) {}
