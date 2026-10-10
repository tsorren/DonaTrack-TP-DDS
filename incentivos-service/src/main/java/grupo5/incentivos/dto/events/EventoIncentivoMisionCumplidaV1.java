package grupo5.incentivos.dto.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoIncentivoMisionCumplidaV1(
    UUID personaDonanteId, LocalDateTime fecha, String nombreMision, String recompensa) {}
