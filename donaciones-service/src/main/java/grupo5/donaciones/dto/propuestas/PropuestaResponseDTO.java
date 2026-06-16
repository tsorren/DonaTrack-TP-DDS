package grupo5.donaciones.dto.propuestas;

public record PropuestaResponseDTO(
    Long id, String estado, String necesidadDescripcion, Long donacionIndependienteId) {}
