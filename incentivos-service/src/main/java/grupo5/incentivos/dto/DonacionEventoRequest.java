package grupo5.incentivos.dto;

import java.time.LocalDate;

public record DonacionEventoRequest(
    Long donanteId,
    String nombreUsuario,
    Long donacionId,
    Long organizacionId,
    String subcategoria,
    Integer cantidadBienes,
    LocalDate fecha,
    boolean exitosa) {}
