package grupo5.donaciones.dto.mediosDeContacto;

public record WhatsAppOutputDTO(
    Boolean esPredeterminado, String caracteristica, String codigoArea, String numero)
    implements MedioDeContactoOutputDTO {}
