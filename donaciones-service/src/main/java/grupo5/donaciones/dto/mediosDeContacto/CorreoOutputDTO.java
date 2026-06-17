package grupo5.donaciones.dto.mediosDeContacto;

public record CorreoOutputDTO(Boolean esPredeterminado, String direccionCorreo)
    implements MedioDeContactoOutputDTO {}
