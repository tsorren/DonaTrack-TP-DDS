package grupo5.common.repositories;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class CrudRepositoryEnMemoria<T extends AggregateRoot>
    implements CrudRepository<T> {
  protected final Map<UUID, T> storage = new ConcurrentHashMap<>();

  @Override
  public T save(T aggregate) {
    if (aggregate == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (aggregate.getId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    storage.put(aggregate.getId(), aggregate);
    return aggregate;
  }

  @Override
  public List<T> saveAll(List<T> aggregates) {
    if (aggregates == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    Map<UUID, T> map = aggregates.stream().collect(Collectors.toMap(T::getId, Function.identity()));
    storage.putAll(map);
    return aggregates;
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
    if (aggregate == null || aggregate.getId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    storage.remove(aggregate.getId());
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
