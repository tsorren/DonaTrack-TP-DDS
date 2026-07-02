package grupo5.notificaciones.dto;

import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;

public record PersonaReplicaDTO(
    UUID id,
    String denominacion,
    TipoPersona tipoPersona,
    List<MedioDeContactoReplicaDTO> mediosDeContacto) {}
