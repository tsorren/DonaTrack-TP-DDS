package grupo5.common.donaciones.donaciones.events;

import java.time.LocalDate;

public record ItemDonacionDTO(
    String descripcion,
    String categoria,
    String subcategoria,
    int cantidad,
    String unidad,
    boolean usado,
    LocalDate fechaVencimiento) {}
