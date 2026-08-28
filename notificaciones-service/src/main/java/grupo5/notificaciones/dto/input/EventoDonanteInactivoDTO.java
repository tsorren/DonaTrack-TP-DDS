package grupo5.notificaciones.dto.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonanteInactivoDTO(
    @NotNull(message = "El ID del donante es obligatorio") UUID idPersonaDonante,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDateTime fecha,
    @NotNull(message = "Los días de inactividad son obligatorios")
        @Positive(message = "Los días de inactividad deben ser mayores a cero")
        Integer diasInactivo)
    implements EventoNotificableDTO {}
