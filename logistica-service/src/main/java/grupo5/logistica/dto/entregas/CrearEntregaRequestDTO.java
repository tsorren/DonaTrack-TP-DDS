package grupo5.logistica.dto.entregas;

import grupo5.logistica.dto.rutas.DireccionDTO;
import java.util.UUID;

public record CrearEntregaRequestDTO(
    UUID idDonacion,
    UUID idBeneficiaria,
    DireccionDTO destino,
    Float pesoTotalKG,
    Float volumenTotalM3) {}
