package grupo5.donaciones.dto.donantes;

public record ActualizarDonanteRequest(
    String tipoPersona,
    String tipoDocumento,
    String documento,
    String denominacion,
    String email,
    String telefono) {}
