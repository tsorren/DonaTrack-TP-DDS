package grupo5.notificaciones.dto.contacto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/contacto/{numero}/respuesta}. */
public record RespuestaContactoInputDTO(
    @NotBlank(message = "El texto de la respuesta es obligatorio")
        @Size(max = 5000, message = "La respuesta tiene hasta 5000 caracteres")
        String texto) {}
