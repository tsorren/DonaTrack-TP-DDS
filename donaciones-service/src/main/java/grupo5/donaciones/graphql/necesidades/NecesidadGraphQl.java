package grupo5.donaciones.graphql.necesidades;

import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import java.util.UUID;

/**
 * El tipo {@code Necesidad} del esquema, sacado de {@link NecesidadPublicaDTO} con los nombres del
 * contrato (§4.1): {@code idEntidad} pasa a {@code entidadId} e {@code idSubcategoria} a {@code
 * subcategoriaId}. {@code fechaFin} va como texto ISO-8601 ({@code AAAA-MM-DD}). La relación {@code
 * entidad} la resuelve aparte, por lotes, el lote {@code g2-relaciones} a partir de {@code
 * entidadId}.
 */
public record NecesidadGraphQl(
    UUID id,
    UUID entidadId,
    UUID subcategoriaId,
    String descripcion,
    Integer cantidadNecesitada,
    String tipo,
    String fechaFin) {

  public static NecesidadGraphQl de(NecesidadPublicaDTO dto) {
    return new NecesidadGraphQl(
        dto.id(),
        dto.idEntidad(),
        dto.idSubcategoria(),
        dto.descripcion(),
        dto.cantidadNecesitada(),
        dto.tipo(),
        dto.fechaFin() == null ? null : dto.fechaFin().toString());
  }
}
