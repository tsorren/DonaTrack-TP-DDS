package grupo5.donaciones.dto.categorias;

import grupo5.donaciones.models.entities.categorias.Unidad;

public record CategoriaInputDTO(
    String nombre, boolean conUso, boolean conVencimiento, Unidad unidad) {}
