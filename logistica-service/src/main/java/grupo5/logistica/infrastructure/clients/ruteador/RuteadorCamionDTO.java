package grupo5.logistica.infrastructure.clients.ruteador;

import java.util.UUID;

public record RuteadorCamionDTO(
    UUID camionId, String patente, Float capacidadVolumen, Float altura, Float capacidadKG) {}
