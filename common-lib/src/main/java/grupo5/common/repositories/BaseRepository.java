package grupo5.common.repositories;

import java.util.List;
import java.util.Optional;

public interface BaseRepository<Recurso, ID> {
  List<Recurso> findAll();

  Optional<Recurso> findById(ID id);

  Recurso save(ID id, Recurso recurso);

  void deleteById(ID id);

  boolean existsById(ID id);
}
