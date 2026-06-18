package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NuevaDonacionRequest(
    UUID donanteId,
    List<String> categorias,
    Integer cantidadBienes,
    LocalDate fecha,
    String nombreDonante) {}
