package grupo5.common.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseRepository<T extends RecursoDTO> {
  List<T> findAll();

  Optional<T> findById(UUID id);

  T save(UUID id, T recurso);

  void deleteById(UUID id);

  boolean existsById(UUID id);

  long count();

  void deleteAll();
}
