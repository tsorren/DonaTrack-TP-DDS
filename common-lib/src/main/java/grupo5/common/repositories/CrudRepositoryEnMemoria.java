package grupo5.common.repositories;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CrudRepositoryEnMemoria<T extends AggregateRoot>
    implements CrudRepository<T> {
  protected final Map<UUID, T> storage = new ConcurrentHashMap<>();

  @Override
  public T save(T aggregate) {
    if (aggregate == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (aggregate.id() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    storage.put(aggregate.id(), aggregate);
    return aggregate;
  }

  @Override
  public List<T> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public Optional<T> findById(UUID id) {
    if (id == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public void delete(T aggregate) {
    if (aggregate == null || aggregate.id() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    storage.remove(aggregate.id());
  }

  @Override
  public boolean existsById(UUID id) {
    if (id == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    return storage.containsKey(id);
  }

  @Override
  public long count() {
    return storage.size();
  }

  @Override
  public void deleteAll() {
    storage.clear();
  }
}
