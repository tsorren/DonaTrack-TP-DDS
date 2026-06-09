package grupo5.common.donaciones.donaciones.commands;

import java.time.LocalDate;

public record ItemCargaDTO(
    String descripcion,
    String categoria,
    String subcategoria,
    Integer cantidad,
    String unidad,
    Boolean usado,
    LocalDate fechaVencimiento) {}
