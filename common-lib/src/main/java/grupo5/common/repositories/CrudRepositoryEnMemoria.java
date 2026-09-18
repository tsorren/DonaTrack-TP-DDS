package grupo5.common.repositories;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CrudRepositoryEnMemoria<T extends AggregateRoot>
    implements CrudRepository<T> {
  private final Logger log = LoggerFactory.getLogger(getClass());
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
    log.info(
        "[REPOSITORY] [ACTION: SAVE] [ENTITY: {}] [ID: {}] - Entity saved successfully",
        aggregate.getClass().getSimpleName(),
        aggregate.getId());
    return aggregate;
  }

  @Override
  public List<T> saveAll(List<T> aggregates) {
    if (aggregates == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    Map<UUID, T> map = aggregates.stream().collect(Collectors.toMap(T::getId, Function.identity()));
    storage.putAll(map);
    String entityName =
        aggregates.isEmpty() ? "Unknown" : aggregates.get(0).getClass().getSimpleName();
    log.info(
        "[REPOSITORY] [ACTION: SAVE_ALL] [ENTITY: {}] [COUNT: {}] - Multiple entities saved successfully",
        entityName,
        aggregates.size());
    return aggregates;
  }

  @Override
  public List<T> findAll() {
    log.debug("[REPOSITORY] [ACTION: FIND_ALL] - Retrieving all entities");
    return new ArrayList<>(storage.values());
  }

  @Override
  public Optional<T> findById(UUID id) {
    if (id == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    log.debug("[REPOSITORY] [ACTION: FIND_BY_ID] [ID: {}] - Retrieving entity by ID", id);
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public void delete(T aggregate) {
    if (aggregate == null || aggregate.getId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    storage.remove(aggregate.getId());
    log.info(
        "[REPOSITORY] [ACTION: DELETE] [ENTITY: {}] [ID: {}] - Entity deleted successfully",
        aggregate.getClass().getSimpleName(),
        aggregate.getId());
  }

  @Override
  public boolean existsById(UUID id) {
    if (id == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    boolean exists = storage.containsKey(id);
    log.debug(
        "[REPOSITORY] [ACTION: EXISTS_BY_ID] [ID: {}] [EXISTS: {}] - Checking entity existence",
        id,
        exists);
    return exists;
  }

  @Override
  public long count() {
    long total = storage.size();
    log.debug("[REPOSITORY] [ACTION: COUNT] [TOTAL: {}] - Counting all entities", total);
    return total;
  }

  @Override
  public void deleteAll() {
    storage.clear();
    log.info("[REPOSITORY] [ACTION: DELETE_ALL] - All entities deleted successfully");
  }
}
