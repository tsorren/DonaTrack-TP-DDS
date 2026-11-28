package grupo5.donaciones.graphql.entidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.UUID;

/**
 * El {@code input FiltroEntidades} del esquema: los mismos filtros que admite {@code
 * buscarPublicas}. Un campo nulo no filtra.
 *
 * @param nombre contenido en la razón social; el largo lo valida el servicio
 * @param categoriaId el {@code ID} de la categoría, en texto
 */
public record FiltroEntidades(String nombre, String categoriaId) {

  /** La categoría como UUID; {@code null} si no vino. Un texto que no es UUID da ERR-CSR-003. */
  public UUID categoria() {
    if (categoriaId == null) {
      return null;
    }
    try {
      return UUID.fromString(categoriaId);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
