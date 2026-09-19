package grupo5.incentivos.dto.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoIncentivoSubioCategoriaV1(
    UUID personaDonanteId,
    LocalDateTime fecha,
    String nombreNuevaCategoria,
    String nombreViejaCategoria) {}
