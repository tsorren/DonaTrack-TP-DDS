package grupo5.incentivos.dto.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoIncentivoSubioCategoriaV1(
    UUID idPersona, LocalDateTime fecha, String categoriaNueva, String categoriaVieja) {}
