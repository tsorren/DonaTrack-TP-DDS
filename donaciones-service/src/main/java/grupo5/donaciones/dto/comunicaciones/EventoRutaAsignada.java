package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoRutaAsignada(
    UUID rutaId, UUID donacionIndependienteId, LocalDateTime fechaAsignacion) {}
