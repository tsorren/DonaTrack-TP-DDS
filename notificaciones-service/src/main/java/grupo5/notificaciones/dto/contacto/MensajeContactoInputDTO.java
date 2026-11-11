package grupo5.notificaciones.dto.contacto;

import grupo5.notificaciones.models.entities.contacto.TemaContacto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Cuerpo de {@code POST /api/contacto}. {@code codigoError} tiene el formato del código que
 * muestran los avisos de error del sitio (el traceId del pedido: 32 caracteres hexadecimales; los
 * ejemplos de la maqueta muestran 12).
 */
public record MensajeContactoInputDTO(
    @NotNull(message = "El tema es obligatorio") TemaContacto tema,
    @NotBlank(message = "El texto es obligatorio")
        @Size(max = 2000, message = "El texto tiene hasta 2000 caracteres")
        String texto,
    @Pattern(
            regexp = "[0-9a-fA-F]{12,32}",
            message = "El código de error tiene de 12 a 32 caracteres hexadecimales")
        String codigoError,
    @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 254, message = "El correo tiene hasta 254 caracteres")
        String correo,
    @NotNull(message = "La clave de envío es obligatoria") UUID claveEnvio) {}
