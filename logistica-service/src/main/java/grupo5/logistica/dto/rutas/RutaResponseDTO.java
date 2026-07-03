package grupo5.logistica.dto.rutas;

import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RutaResponseDTO(
    UUID id,
    LocalDate fecha,
    List<UUID> entregaIds,
    UUID choferId,
    UUID camionId,
    EstadoRuta estado,
    LocalDateTime horaInicioReal,
    LocalDateTime horaFinReal,
    String urlSeguimiento) {}
