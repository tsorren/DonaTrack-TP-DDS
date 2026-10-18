package grupo5.notificaciones.dto;

import grupo5.notificaciones.models.entities.personas.TipoPersona;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PersonaReplicaDTO(
    @NotNull(message = "El ID de la persona es obligatorio") UUID id,
    @NotBlank(message = "La denominación es obligatoria") String denominacion,
    @NotNull(message = "El tipo de persona es obligatorio") TipoPersona tipoPersona,
    List<@Valid MedioDeContactoReplicaDTO> mediosDeContacto) {}
