package grupo5.common.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrudRepository<T extends AggregateRoot> {
  T save(T aggregate);

  List<T> saveAll(List<T> aggregates);

  List<T> findAll();

  Optional<T> findById(UUID id);

  void delete(T aggregate);

  boolean existsById(UUID id);

  long count();

  void deleteAll();
}
