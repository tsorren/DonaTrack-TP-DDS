package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.common.repositories.AggregateRoot;
import grupo5.common.repositories.CrudRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
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
  protected final Function<D, T> toDomain;
  private final BiFunction<T, D, D> toDocument;

  /**
   * @param toDocument recibe el agregado y el documento ya guardado ({@code null} si es nuevo),
   *     para conservar lo que el dominio no trae y verificar la versión
   */
  protected CrudRepositoryMongoAdapter(
      R springDataRepo, BiFunction<T, D, D> toDocument, Function<D, T> toDomain) {
    this.springDataRepo = springDataRepo;
    this.toDocument = toDocument;
    this.toDomain = toDomain;
  }

  @Override
  public T save(T aggregate) {
    if (aggregate == null) {
      return null;
    }
    D documento = toDocument.apply(aggregate, documentoGuardado(aggregate.getId()));
    completarAntesDeGuardar(documento);
    return toDomain.apply(springDataRepo.save(documento));
  }

  /** Para completar en el documento los datos desnormalizados que el agregado no trae. */
  protected void completarAntesDeGuardar(D documento) {}

  @Override
  public List<T> saveAll(List<T> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<T> findAll() {
    return aDominio(springDataRepo.findAll());
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

  protected List<T> aDominio(List<D> documentos) {
    return documentos.stream().map(toDomain).toList();
  }

  private D documentoGuardado(UUID id) {
    return id != null ? springDataRepo.findById(id).orElse(null) : null;
  }
}
