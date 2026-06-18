package grupo5.donaciones.dto.comunicaciones;

public record MedioDeContactoReplicaDTO(
    String tipo,
    Boolean esPredeterminado,
    String direccionCorreo,
    String caracteristica,
    String codigoArea,
    String numero) {}
