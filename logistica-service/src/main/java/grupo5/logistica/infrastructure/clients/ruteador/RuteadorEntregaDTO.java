package grupo5.logistica.infrastructure.clients.ruteador;

import java.util.UUID;

public record RuteadorEntregaDTO(
    UUID entregaId,
    UUID idDonacion,
    UUID idBeneficiaria,
    Float pesoTotalKG,
    Float volumenTotalM3) {}
