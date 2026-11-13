package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;

/**
 * Lo que la propuesta pide de una donación. Desde R11-24 (aditivo): {@code disponible} (lo que
 * queda de ella en el depósito; 0 si ya no está), {@code pedidaEnOtras} (lo que piden de ella las
 * otras propuestas pendientes) y {@code otrasPropuestas} (cuántas otras pendientes la piden).
 */
public record GrupoFragmentacionDTO(
    DonacionIndependienteResponseDTO donacionIndependiente,
    Integer cantidadNecesaria,
    int disponible,
    int pedidaEnOtras,
    int otrasPropuestas) {}
