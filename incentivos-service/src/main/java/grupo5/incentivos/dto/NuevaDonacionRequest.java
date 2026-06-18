package grupo5.incentivos.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NuevaDonacionRequest(
    UUID donanteId,
    List<String> categorias,
    Integer cantidadBienes,
    LocalDate fecha,
    String nombreDonante) {}
