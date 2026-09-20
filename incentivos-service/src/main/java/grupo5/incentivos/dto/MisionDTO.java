package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import java.time.LocalDate;

/**
 * DTO anémico: el mapeo (y la resolución de qué insignia mostrar) vive en {@link
 * grupo5.incentivos.services.mappers.MisionMapper}, no acá.
 */
public record MisionDTO(
    String nombre,
    String descripcion,
    CategoriaDonante categoria,
    int progresoActual,
    int objetivo,
    int porcentaje,
    int distanciaAlObjetivo,
    boolean completada,
    LocalDate fechaCompletada,
    InsigniaDTO insignia) {}
