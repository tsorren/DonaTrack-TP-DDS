package grupo5.donaciones.dto.itemsNormalizados.inputs;

import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import java.util.UUID;

public record ItemDonacionNormalizadoPatchDTO(
    EstadoNormalizacion estadoNormalizacion, UUID subcategoriaId) {}
