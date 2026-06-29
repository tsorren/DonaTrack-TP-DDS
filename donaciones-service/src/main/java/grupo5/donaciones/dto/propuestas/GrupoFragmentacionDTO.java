package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;

public record GrupoFragmentacionDTO(
    DonacionIndependienteResponseDTO donacionIndependiente, Integer cantidadNecesaria) {}
