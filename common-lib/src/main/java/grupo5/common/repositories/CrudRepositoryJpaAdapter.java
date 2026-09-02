package grupo5.common.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador base reutilizable para repositorios de persistencia basados en Spring Data JPA.
 *
 * @param <T> Tipo del Agregado de Dominio (debe implementar AggregateRoot)
 * @param <E> Tipo de la Entidad JPA de infraestructura
 * @param <R> Tipo de la interfaz Spring Data JPA
 */
public abstract class CrudRepositoryJpaAdapter<
        T extends AggregateRoot, E, R extends JpaRepository<E, UUID>>
    implements CrudRepository<T> {

  protected final R springDataRepo;
  protected final Function<T, E> toEntity;
  protected final Function<E, T> toDomain;

  protected CrudRepositoryJpaAdapter(
      R springDataRepo, Function<T, E> toEntity, Function<E, T> toDomain) {
    this.springDataRepo = springDataRepo;
    this.toEntity = toEntity;
    this.toDomain = toDomain;
  }

  @Override
  public T save(T aggregate) {
    E entity = toEntity.apply(aggregate);
    E saved = springDataRepo.save(entity);
    return toDomain.apply(saved);
  }

  @Override
  public List<T> saveAll(List<T> aggregates) {
    List<E> entities = aggregates.stream().map(toEntity).toList();
    return springDataRepo.saveAll(entities).stream().map(toDomain).toList();
  }

  @Override
  public List<T> findAll() {
    return springDataRepo.findAll().stream().map(toDomain).toList();
  }

  @Override
  public Optional<T> findById(UUID id) {
    return springDataRepo.findById(id).map(toDomain);
  }

  @Override
  public void delete(T aggregate) {
    springDataRepo.deleteById(aggregate.getId());
  }

  @Override
  public boolean existsById(UUID id) {
    return springDataRepo.existsById(id);
  }

  @Override
  public long count() {
    return springDataRepo.count();
  }

  @Override
  public void deleteAll() {
    springDataRepo.deleteAll();
  }
}
