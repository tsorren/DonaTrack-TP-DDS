package grupo5.donaciones.dto.donaciones.outputs;

import grupo5.donaciones.models.entities.donaciones.Estado;
import java.time.LocalDate;

public record ItemDonacionOutputDTO(
    String descripcionBien,
    String fotoUrl,
    LocalDate fechaVencimiento,
    Estado estadoBien,
    Double pesoUnitario,
    Double volumenUnitario,
    Integer cantidad) {}
