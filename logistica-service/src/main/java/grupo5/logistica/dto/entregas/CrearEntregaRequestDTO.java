package grupo5.logistica.dto.entregas;

import grupo5.logistica.dto.rutas.DireccionDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CrearEntregaRequestDTO(
    @NotNull UUID idDonacion,
    @NotNull UUID idBeneficiaria,
    @NotNull @Valid DireccionDTO destino,
    @NotNull @Positive Float pesoTotalKG,
    @NotNull @Positive Float volumenTotalM3,
    UUID donanteId) {

  /** Pedido sin donante: el {@code donanteId} es opcional (ADR de réplicas por evento, HB-16). */
  public CrearEntregaRequestDTO(
      UUID idDonacion,
      UUID idBeneficiaria,
      DireccionDTO destino,
      Float pesoTotalKG,
      Float volumenTotalM3) {
    this(idDonacion, idBeneficiaria, destino, pesoTotalKG, volumenTotalM3, null);
  }
}
