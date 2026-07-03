package grupo5.donaciones.dto.comunicaciones;

import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import java.util.UUID;

public record NuevaEntregaRequest(
    UUID idDonacion,
    UUID idBeneficiaria,
    DireccionOutputDTO destino,
    Double pesoTotalKG,
    Double volumenTotalM3) {}
