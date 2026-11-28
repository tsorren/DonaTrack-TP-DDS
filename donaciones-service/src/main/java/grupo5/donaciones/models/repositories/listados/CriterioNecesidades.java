package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Búsqueda de necesidades vigentes en {@code hoy} (SPEC-11, G2): activas, sin vencer y no
 * satisfechas con lo asignado a su período actual, las mismas que {@code listarPublicas}. Un campo
 * nulo no filtra; los que vienen se combinan con Y. {@code categoriaId}: las de alguna subcategoría
 * de esa categoría. El orden es fijo: por descripción sin mayúsculas ni tildes, desempata por id.
 */
public record CriterioNecesidades(
    UUID entidadId,
    UUID categoriaId,
    UUID subcategoriaId,
    TipoNecesidad tipo,
    LocalDate hoy,
    Paginado paginado) {}
