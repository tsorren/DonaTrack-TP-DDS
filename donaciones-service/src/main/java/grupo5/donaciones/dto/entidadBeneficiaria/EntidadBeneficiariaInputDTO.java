package grupo5.donaciones.dto.entidadBeneficiaria;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EntidadBeneficiariaInputDTO(
    @NotNull(message = "El ID de persona jurídica es obligatorio") UUID juridicaId) {}
