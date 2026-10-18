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
    @NotNull @Positive Float volumenTotalM3) {}
