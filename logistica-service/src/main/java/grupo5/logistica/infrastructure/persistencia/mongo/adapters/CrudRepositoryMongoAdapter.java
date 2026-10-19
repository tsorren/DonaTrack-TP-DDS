package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.common.repositories.AggregateRoot;
import grupo5.common.repositories.CrudRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Adaptador base reutilizable para repositorios de persistencia basados en Spring Data MongoDB.
 *
 * @param <T> Tipo del Agregado de Dominio (debe implementar AggregateRoot)
 * @param <D> Tipo del Documento MongoDB de infraestructura
 * @param <R> Tipo de la interfaz Spring Data MongoDB
 */
@SuppressWarnings("java:S120")
public abstract class CrudRepositoryMongoAdapter<
        T extends AggregateRoot, D, R extends MongoRepository<D, UUID>>
    implements CrudRepository<T> {

  protected final R springDataRepo;
  protected final Function<T, D> toDocument;
  protected final Function<D, T> toDomain;

  protected CrudRepositoryMongoAdapter(
      R springDataRepo, Function<T, D> toDocument, Function<D, T> toDomain) {
    this.springDataRepo = springDataRepo;
    this.toDocument = toDocument;
    this.toDomain = toDomain;
  }

  @Override
  public T save(T aggregate) {
    D document = toDocument.apply(aggregate);
    D saved = springDataRepo.save(document);
    return toDomain.apply(saved);
  }

  @Override
  public List<T> saveAll(List<T> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    List<D> documents = aggregates.stream().map(toDocument).toList();
    return springDataRepo.saveAll(documents).stream().map(toDomain).toList();
  }

  @Override
  public List<T> findAll() {
    return springDataRepo.findAll().stream().map(toDomain).toList();
  }

  @Override
  public Optional<T> findById(UUID id) {
    if (id == null) {
      return Optional.empty();
    }
    return springDataRepo.findById(id).map(toDomain);
  }

  @Override
  public void delete(T aggregate) {
    if (aggregate != null && aggregate.getId() != null) {
      springDataRepo.deleteById(aggregate.getId());
    }
  }

  @Override
  public boolean existsById(UUID id) {
    if (id == null) {
      return false;
    }
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
