package grupo5.donaciones.dto.categorias;

import grupo5.donaciones.models.entities.categorias.Unidad;
import java.util.List;
import java.util.UUID;

public record CategoriaOutputDTO(
    UUID id,
    String nombre,
    boolean conUso,
    boolean conVencimiento,
    Unidad unidad,
    List<SubcategoriaOutputDTO> subcategorias) {}
