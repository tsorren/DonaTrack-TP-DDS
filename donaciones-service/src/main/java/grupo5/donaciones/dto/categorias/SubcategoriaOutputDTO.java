package grupo5.donaciones.dto.categorias;

import java.util.List;
import java.util.UUID;

public record SubcategoriaOutputDTO(
    UUID id,
    String nombre,
    CategoriaOutputDTO categoria,
    List<AliasSubcategoriaOutputDTO> aliases) {}
