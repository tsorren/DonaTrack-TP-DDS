package grupo5.notificaciones.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonanteRegistradoDTO(
    @NotNull(message = "El ID del donante es obligatorio") UUID idPersonaDonante,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDateTime fecha,
    @NotBlank(message = "Las credenciales de acceso son obligatorias") String credencialesDeAcceso)
    implements EventoNotificableDTO {}
