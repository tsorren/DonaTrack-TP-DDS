package grupo5.donaciones.dto.categorias;

import java.util.List;
import java.util.UUID;

public record SubcategoriaInputDTO(
    String nombre, UUID idCategoria, List<AliasSubcategoriaInputDTO> aliases) {}
