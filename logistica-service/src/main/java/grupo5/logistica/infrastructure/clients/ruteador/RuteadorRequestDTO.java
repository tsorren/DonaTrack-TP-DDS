package grupo5.logistica.infrastructure.clients.ruteador;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RuteadorRequestDTO(
    UUID solicitudId,
    UUID correlationId,
    LocalDate fecha,
    String callbackUrl,
    List<RuteadorEntregaDTO> entregas,
    List<RuteadorCamionDTO> camiones) {}
