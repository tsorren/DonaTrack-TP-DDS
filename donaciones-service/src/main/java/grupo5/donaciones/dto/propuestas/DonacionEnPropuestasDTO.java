package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;

/**
 * Una donación independiente que está en al menos una propuesta pendiente (R11-24): lo que queda de
 * ella en el depósito, cuánto piden las pendientes, en cuántas está y si alcanza para todas.
 */
public record DonacionEnPropuestasDTO(
    DonacionIndependienteResponseDTO donacionIndependiente,
    int disponible,
    int pedidaEnPendientes,
    int propuestasPendientes,
    boolean alcanza) {}
