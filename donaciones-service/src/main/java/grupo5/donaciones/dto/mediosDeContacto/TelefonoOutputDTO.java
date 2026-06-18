package grupo5.donaciones.dto.mediosDeContacto;

public record TelefonoOutputDTO(
    Boolean esPredeterminado, String caracteristica, String codigoArea, String numero)
    implements MedioDeContactoOutputDTO {}
