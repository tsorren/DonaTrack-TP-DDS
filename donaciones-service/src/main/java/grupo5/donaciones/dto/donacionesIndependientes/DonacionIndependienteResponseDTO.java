package grupo5.donaciones.dto.donacionesIndependientes;

import java.util.List;
import java.util.UUID;

public record DonacionIndependienteResponseDTO(
    UUID id,
    String estadoActual,
    List<String> historialEstados,
    List<ItemDonacionIndependienteResponseDTO> items,
    int stockActual) {}
