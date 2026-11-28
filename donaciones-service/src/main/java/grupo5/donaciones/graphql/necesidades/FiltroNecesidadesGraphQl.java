package grupo5.donaciones.graphql.necesidades;

import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import java.util.UUID;

/**
 * El {@code input FiltroNecesidades} del esquema. Los ids llegan como texto y se leen acá: un id
 * mal escrito es {@code IllegalArgumentException}, que la traducción de errores devuelve como
 * {@code ERR-CSR-003}.
 */
public record FiltroNecesidadesGraphQl(
    String entidadId, String categoriaId, String subcategoriaId, TipoNecesidad tipo) {

  UUID entidad() {
    return uuid(entidadId);
  }

  UUID categoria() {
    return uuid(categoriaId);
  }

  UUID subcategoria() {
    return uuid(subcategoriaId);
  }

  private static UUID uuid(String id) {
    return id == null ? null : UUID.fromString(id);
  }
}
