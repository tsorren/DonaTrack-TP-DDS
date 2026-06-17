package grupo5.incentivos.dto;

import java.time.LocalDate;
import java.util.List;

public record NuevaDonacionRequest(
    Long donanteId,
    List<String> categorias,
    Integer cantidadBienes,
    LocalDate fecha,
    String nombreDonante) {}
