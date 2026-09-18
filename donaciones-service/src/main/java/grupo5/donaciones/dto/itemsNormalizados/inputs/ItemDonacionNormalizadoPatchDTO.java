package grupo5.donaciones.dto.itemsNormalizados.inputs;

import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ItemDonacionNormalizadoPatchDTO(
    @NotNull(message = "El estado de normalización es obligatorio")
        EstadoNormalizacion estadoNormalizacion,
    UUID subcategoriaId) {}
