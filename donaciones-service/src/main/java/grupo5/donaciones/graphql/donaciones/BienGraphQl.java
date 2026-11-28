package grupo5.donaciones.graphql.donaciones;

import grupo5.donaciones.models.entities.donaciones.Estado;

/** El tipo {@code Bien} del esquema; la fecha de vencimiento va como texto {@code AAAA-MM-DD}. */
public record BienGraphQl(
    String descripcion, String fotoUrl, String fechaVencimiento, Estado estado) {}
