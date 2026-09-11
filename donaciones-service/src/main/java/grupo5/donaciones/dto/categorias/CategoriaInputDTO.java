package grupo5.donaciones.dto.categorias;

import grupo5.donaciones.models.entities.categorias.Unidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoriaInputDTO(
    @NotBlank(message = "El nombre de la categoría es obligatorio") String nombre,
    boolean conUso,
    boolean conVencimiento,
    @NotNull(message = "La unidad es obligatoria") Unidad unidad) {}
