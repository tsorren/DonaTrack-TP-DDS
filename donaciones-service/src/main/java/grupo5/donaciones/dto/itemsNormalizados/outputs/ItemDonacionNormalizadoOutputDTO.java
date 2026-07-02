package grupo5.donaciones.dto.itemsNormalizados.outputs;

import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import java.util.UUID;

public record ItemDonacionNormalizadoOutputDTO(
    UUID id,
    UUID donacionOriginalId,
    String descripcionBienOriginal,
    Integer cantidad,
    SubcategoriaOutputDTO subcategoria,
    Double confianza,
    EstadoNormalizacion estadoNormalizacion,
    Boolean segmentado) {}
