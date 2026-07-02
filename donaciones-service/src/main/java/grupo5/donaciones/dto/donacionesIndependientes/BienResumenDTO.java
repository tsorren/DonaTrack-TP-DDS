package grupo5.donaciones.dto.donacionesIndependientes;

import grupo5.donaciones.models.entities.donaciones.Estado;
import java.time.LocalDate;

public record BienResumenDTO(
    String descripcion, String fotoUrl, LocalDate fechaVencimiento, Estado estado) {}
