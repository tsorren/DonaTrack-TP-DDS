package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.Set;
import java.util.UUID;

/**
 * Búsqueda de personas (R11-01), ordenada por nombre visible. Un campo nulo no filtra; {@code ids}
 * vacío tampoco. Con {@code ids} es una lectura por lote: solo esas personas.
 */
public record CriterioPersonas(
    String q, TipoPersona tipo, TipoJuridico tipoJuridico, Set<UUID> ids, Paginado paginado) {

  public CriterioPersonas {
    ids = ids == null ? Set.of() : Set.copyOf(ids);
  }
}
