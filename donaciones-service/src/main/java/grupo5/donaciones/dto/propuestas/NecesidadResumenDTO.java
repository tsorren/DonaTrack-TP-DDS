package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.categorias.Unidad;
import java.util.UUID;

/**
 * La necesidad que cubre una propuesta. Desde R11-24 (aditivo): la entidad que la pidió ({@code
 * entidadId} y {@code entidadNombre}, su razón social) y la unidad en que se pide.
 */
public record NecesidadResumenDTO(
    UUID id,
    String descripcion,
    Integer cantidadNecesitada,
    String tipo,
    String estado,
    UUID entidadId,
    String entidadNombre,
    Unidad unidad) {

  /** Sin los datos de R11-24 (los tests de antes de ese cambio). */
  public NecesidadResumenDTO(
      UUID id, String descripcion, Integer cantidadNecesitada, String tipo, String estado) {
    this(id, descripcion, cantidadNecesitada, tipo, estado, null, null, null);
  }
}
