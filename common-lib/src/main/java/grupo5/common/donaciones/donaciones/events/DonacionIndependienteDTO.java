package grupo5.common.donaciones.donaciones.events;

import java.time.LocalDate;
import java.util.UUID;

public record DonacionIndependienteDTO(
    UUID donacionId,
    String subcategoria,
    int cantidad,
    boolean usado,
    LocalDate fechaVencimiento) {}
