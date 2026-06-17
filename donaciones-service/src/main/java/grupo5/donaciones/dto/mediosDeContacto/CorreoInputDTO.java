package grupo5.donaciones.dto.mediosDeContacto;

public record CorreoInputDTO(Boolean esPredeterminado, String direccionCorreo)
    implements MedioDeContactoInputDTO {}
