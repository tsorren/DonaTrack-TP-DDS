package grupo5.donaciones.dto.replicas;

import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;

public record PersonaReplicaDTO(
    UUID id,
    String denominacion,
    TipoPersona tipoPersona,
    List<MedioDeContactoReplicaDTO> mediosDeContacto) {}
