package grupo5.donaciones.models.repositories.listados;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lectura de varios agregados por id en una sola consulta, para armar una página sin una lectura
 * por fila. Los ids que no existen no aparecen; el orden del resultado no está garantizado.
 */
public interface LecturaPorLote<T> {

  Optional<T> findById(UUID id);

  /** Por defecto, una lectura por id: los repositorios en memoria. Los JPA la redefinen. */
  default List<T> findAllById(Collection<UUID> ids) {
    return ids.stream().distinct().map(this::findById).flatMap(Optional::stream).toList();
  }
}
