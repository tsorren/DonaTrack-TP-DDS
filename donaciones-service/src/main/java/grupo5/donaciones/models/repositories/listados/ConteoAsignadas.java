package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.util.UUID;

/**
 * Cuántas donaciones asignadas a una organización hay de una necesidad en un estado (R11-43). La
 * necesidad es nula en las asignadas sin necesidad.
 */
public record ConteoAsignadas(UUID necesidadId, TipoEstadoDonacion estado, long cantidad) {}
