package grupo5.donaciones.dto.entidadBeneficiaria;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * El alta de una organización. {@code origen} es opcional desde R11-26 (aditivo): sin él, {@code
 * ADMIN}; solo cambia la acción que queda en el registro de actividad.
 */
public record EntidadBeneficiariaInputDTO(
    @NotNull(message = "El ID de persona jurídica es obligatorio") UUID juridicaId,
    OrigenEntidad origen) {

  public EntidadBeneficiariaInputDTO(UUID juridicaId) {
    this(juridicaId, null);
  }

  /** El origen pedido; sin él, {@code ADMIN}. */
  public OrigenEntidad origenOPorDefecto() {
    return origen == null ? OrigenEntidad.ADMIN : origen;
  }
}
