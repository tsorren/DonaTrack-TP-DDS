package grupo5.donaciones.dto.replicas;

public record MedioDeContactoReplicaDTO(
    String tipo,
    Boolean esPredeterminado,
    String direccionCorreo,
    String caracteristica,
    String codigoArea,
    String numero) {}
