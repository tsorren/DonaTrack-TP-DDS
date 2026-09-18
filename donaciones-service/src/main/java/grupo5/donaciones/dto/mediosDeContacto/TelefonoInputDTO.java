package grupo5.donaciones.dto.mediosDeContacto;

import jakarta.validation.constraints.NotBlank;

public record TelefonoInputDTO(
    Boolean esPredeterminado,
    String caracteristica,
    String codigoArea,
    @NotBlank(message = "El número telefónico es obligatorio") String numero)
    implements MedioDeContactoInputDTO {}
