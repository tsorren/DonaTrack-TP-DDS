package grupo5.donaciones.dto.propuestas;

import java.util.UUID;

public record FragmentacionDTO(
    UUID donacionOriginalId, Integer cantidadNecesaria, String descripcionDonacion) {}
