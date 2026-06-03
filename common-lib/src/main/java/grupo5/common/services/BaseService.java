package grupo5.common.services;

import grupo5.common.errors.RecursoNoEncontradoException;
import grupo5.common.repositories.BaseRepository;
import java.util.List;

public abstract class BaseService<ID, Recurso> {
  private final BaseRepository<ID, Recurso> repository;

  protected BaseService(BaseRepository<ID, Recurso> repository) {
    this.repository = repository;
  }

  public List<Recurso> findAll() {
    return repository.findAll();
  }

  public Recurso findById(ID id) {
    return repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  public Recurso save(ID id, Recurso recurso) {
    return repository.save(id, recurso);
  }

  public Recurso update(ID id, Recurso recurso) {
    if (!repository.existsById(id)) {
      throw new RecursoNoEncontradoException(id);
    }

    return repository.save(id, recurso);
  }

  public void deleteById(ID id) {
    if (!repository.existsById(id)) {
      throw new RecursoNoEncontradoException(id);
    }

    repository.deleteById(id);
  }

  public boolean existsById(ID id) {
    return repository.existsById(id);
  }

  protected BaseRepository<ID, Recurso> repository() {
    return repository;
  }
}