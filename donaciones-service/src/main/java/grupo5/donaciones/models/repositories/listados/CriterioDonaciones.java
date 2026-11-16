package grupo5.donaciones.models.repositories.listados;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Búsqueda de donaciones originales (R11-01). Un campo nulo no filtra. {@code desde} y {@code
 * hasta} acotan la fecha de la donación y son inclusivos. {@code q} busca en su descripción y en la
 * de sus bienes.
 */
public record CriterioDonaciones(
    UUID donanteId,
    LocalDate desde,
    LocalDate hasta,
    String q,
    OrdenPorFecha orden,
    Paginado paginado) {}
