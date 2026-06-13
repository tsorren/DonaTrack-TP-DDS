package grupo5.common.donaciones.donaciones.commands;

import java.util.List;
import java.util.UUID;

public record RegistrarCargaDonacionDTO(
    UUID donanteId, String descripcion, List<ItemCargaDTO> items) {}
