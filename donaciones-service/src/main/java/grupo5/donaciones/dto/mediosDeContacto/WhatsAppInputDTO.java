package grupo5.donaciones.dto.mediosDeContacto;

import jakarta.validation.constraints.NotBlank;

public record WhatsAppInputDTO(
    Boolean esPredeterminado,
    String caracteristica,
    String codigoArea,
    @NotBlank(message = "El número de WhatsApp es obligatorio") String numero)
    implements MedioDeContactoInputDTO {}
