package grupo5.donaciones.dto.donaciones.inputs;

import grupo5.donaciones.models.entities.donaciones.Estado;
import java.time.LocalDate;

public record ItemDonacionInputDTO(
    String descripcionBien,
    String fotoUrl,
    LocalDate fechaVencimiento,
    Estado estadoBien,
    Integer cantidad) {}
