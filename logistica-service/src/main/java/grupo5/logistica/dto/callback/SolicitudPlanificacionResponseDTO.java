package grupo5.logistica.dto.callback;

import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SolicitudPlanificacionResponseDTO(
    UUID id,
    LocalDate fecha,
    EstadoSolicitud estado,
    Integer cantidadDonaciones,
    String callbackUrl,
    List<UUID> rutasGeneradas,
    Integer intentosFallidos,
    String motivoError) {}
