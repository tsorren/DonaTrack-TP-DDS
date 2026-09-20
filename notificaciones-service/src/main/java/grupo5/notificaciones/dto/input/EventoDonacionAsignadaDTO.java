package grupo5.notificaciones.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonacionAsignadaDTO(
    UUID eventId,
    @NotNull(message = "El ID del donante es obligatorio") UUID personaId,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDateTime fecha,
    @NotNull(message = "El ID de la entidad beneficiaria es obligatorio")
        UUID personaBeneficiariaId,
    @NotBlank(message = "La descripción de la donación es obligatoria") String descripcion)
    implements EventoNotificableDTO {}
