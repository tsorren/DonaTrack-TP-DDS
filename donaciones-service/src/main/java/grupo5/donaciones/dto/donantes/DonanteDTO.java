package grupo5.donaciones.dto.donantes;

public record DonanteDTO(
    Long id,
    String tipoPersona,
    String tipoDocumento,
    String documento,
    String denominacion,
    String email,
    String telefono) {}
