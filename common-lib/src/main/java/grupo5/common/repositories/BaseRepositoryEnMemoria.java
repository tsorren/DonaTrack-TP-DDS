package grupo5.common.repositories;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseRepositoryEnMemoria<T extends RecursoDTO> implements BaseRepository<T> {
  protected final Map<UUID, T> storage = new ConcurrentHashMap<>();

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
  public T save(UUID id, T recurso) {
    if (recurso == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    UUID targetId = id;

    // Si el ID del parámetro es nulo, intentamos obtenerlo del recurso (DTO)
    if (targetId == null) {
      targetId = recurso.getId();
    }

    // Si sigue siendo nulo, se trata de una creación. Generamos un nuevo UUID y lo asignamos.
    if (targetId == null) {
      targetId = UUID.randomUUID();
      recurso.setId(targetId);
    } else {
      // Si el ID ya existe (sea en el parámetro o en el recurso), aseguramos que se sincronice en
      // el DTO (actualización)
      recurso.setId(targetId);
    }

    storage.put(targetId, recurso);
    return recurso;
  }

  @Override
  public void deleteById(UUID id) {
    if (id == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    storage.remove(id);
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
