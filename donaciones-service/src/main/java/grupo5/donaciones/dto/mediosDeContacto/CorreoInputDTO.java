package grupo5.donaciones.dto.mediosDeContacto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CorreoInputDTO(
    Boolean esPredeterminado,
    @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Formato de correo electrónico inválido")
        String direccionCorreo)
    implements MedioDeContactoInputDTO {}
