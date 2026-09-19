package grupo5.incentivos.dto.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoIncentivoDonanteInactivoV1(
        UUID idPersona, LocalDateTime fecha, Integer diasInactivo) {}
