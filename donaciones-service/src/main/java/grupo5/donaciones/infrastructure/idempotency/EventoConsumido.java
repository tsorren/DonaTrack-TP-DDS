package grupo5.donaciones.infrastructure.idempotency;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoConsumido(
    UUID id,
    String eventType,
    String queueName,
    UUID businessId,
    UUID donacionId,
    LocalDateTime fechaProcesamiento) {}
