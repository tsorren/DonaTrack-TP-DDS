package grupo5.donaciones.dto.propuestas;

import java.util.UUID;

public record PropuestaResponseDTO(UUID id, String estado, String necesidadDescripcion) {}
