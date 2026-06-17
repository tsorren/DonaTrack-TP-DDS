package grupo5.notificaciones.dto;

public record MedioDeContactoReplicaDTO(
    String tipo,
    Boolean esPredeterminado,
    String direccionCorreo,
    String caracteristica,
    String codigoArea,
    String numero) {}
