package grupo5.notificaciones.dto.contacto;

import grupo5.notificaciones.models.entities.contacto.TemaContacto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Cuerpo de {@code POST /api/contacto}. */
public record MensajeContactoInputDTO(
    @NotNull(message = "El tema es obligatorio") TemaContacto tema,
    @NotBlank(message = "El texto es obligatorio")
        @Size(max = 2000, message = "El texto tiene hasta 2000 caracteres")
        String texto,
    @Size(max = 64, message = "El código de error tiene hasta 64 caracteres") String codigoError,
    @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 254, message = "El correo tiene hasta 254 caracteres")
        String correo,
    @NotNull(message = "La clave de envío es obligatoria") UUID claveEnvio) {}
