package grupo5.donaciones.dto.categorias;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record SubcategoriaInputDTO(
    @NotBlank(message = "El nombre de la subcategoría es obligatorio") String nombre,
    UUID idCategoria,
    List<@Valid AliasSubcategoriaInputDTO> aliases) {}
