package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import java.time.LocalDate;
import java.util.Set;

/**
 * Búsqueda de importaciones de donantes (R11-20). Un campo nulo no filtra; {@code estados} vacío
 * tampoco. {@code desde} y {@code hasta} acotan el día de subida y son inclusivos: con alguno de
 * los dos, las importaciones sin fecha guardada quedan afuera. {@code q} busca en el nombre con que
 * se subió; las que no lo tienen no coinciden con ninguna búsqueda. El orden es por la fecha de
 * subida, con las que no la tienen siempre al final; desempata por id.
 */
public record CriterioArchivos(
    Set<EstadoArchivo> estados,
    LocalDate desde,
    LocalDate hasta,
    String q,
    OrdenPorFecha orden,
    Paginado paginado) {

  public CriterioArchivos {
    estados = estados == null ? Set.of() : Set.copyOf(estados);
  }
}
