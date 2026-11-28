package grupo5.donaciones.dto.publico;

import grupo5.donaciones.dto.AvanceNecesidadDTO;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Necesidad vigente para las rutas públicas (D26). {@code fechaFin} es el fin del período actual en
 * las recurrentes y {@code null} en las extraordinarias. {@code avance}: el del período actual o el
 * total, en unidades (HG-04); opcional.
 */
public record NecesidadPublicaDTO(
    UUID id,
    UUID idEntidad,
    UUID idSubcategoria,
    String descripcion,
    Integer cantidadNecesitada,
    String tipo,
    LocalDate fechaFin,
    AvanceNecesidadDTO avance) {

  /** Los campos de antes de HG-04; el avance queda nulo. */
  public NecesidadPublicaDTO(
      UUID id,
      UUID idEntidad,
      UUID idSubcategoria,
      String descripcion,
      Integer cantidadNecesitada,
      String tipo,
      LocalDate fechaFin) {
    this(id, idEntidad, idSubcategoria, descripcion, cantidadNecesitada, tipo, fechaFin, null);
  }
}
