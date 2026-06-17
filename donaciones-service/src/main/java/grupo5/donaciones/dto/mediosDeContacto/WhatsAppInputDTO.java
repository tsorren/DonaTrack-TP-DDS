package grupo5.donaciones.dto.mediosDeContacto;

public record WhatsAppInputDTO(
    Boolean esPredeterminado, String caracteristica, String codigoArea, String numero)
    implements MedioDeContactoInputDTO {}
