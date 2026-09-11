package grupo5.donaciones.dto.categorias;

import jakarta.validation.constraints.NotBlank;

public record AliasSubcategoriaInputDTO(
    @NotBlank(message = "El alias es obligatorio") String alias) {}
