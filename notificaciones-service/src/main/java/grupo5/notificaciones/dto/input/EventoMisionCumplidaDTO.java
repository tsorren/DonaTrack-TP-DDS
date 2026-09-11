package grupo5.notificaciones.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoMisionCumplidaDTO(
    @NotNull(message = "El ID del donante es obligatorio") UUID idPersonaDonante,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDateTime fecha,
    @NotBlank(message = "El nombre de la misión es obligatorio") String nombreMision,
    @NotBlank(message = "La recompensa es obligatoria") String recompensa)
    implements EventoNotificableDTO {}
