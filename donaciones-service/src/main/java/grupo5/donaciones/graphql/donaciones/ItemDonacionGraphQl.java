package grupo5.donaciones.graphql.donaciones;

import grupo5.donaciones.dto.donacionesIndependientes.CategoriaResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.SubcategoriaResumenDTO;

/**
 * El tipo {@code ItemDonacion} del esquema. {@code Subcategoria} y {@code Categoria} tienen los
 * mismos campos que sus DTO, así que se usan tal cual.
 */
public record ItemDonacionGraphQl(
    Integer cantidad,
    BienGraphQl bien,
    SubcategoriaResumenDTO subcategoria,
    CategoriaResumenDTO categoria) {}
