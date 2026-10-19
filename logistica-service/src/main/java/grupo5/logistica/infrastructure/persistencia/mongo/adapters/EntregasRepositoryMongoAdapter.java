package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataEntregaMongoRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.repositories.IEntregasRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class EntregasRepositoryMongoAdapter
    extends CrudRepositoryMongoAdapter<Entrega, EntregaDocument, SpringDataEntregaMongoRepository>
    implements IEntregasRepository {

  private final EntregaMongoMapper mapper;

  public EntregasRepositoryMongoAdapter(
      SpringDataEntregaMongoRepository springDataRepo, EntregaMongoMapper mapper) {
    super(springDataRepo, mapper::toDocument, mapper::toDomain);
    this.mapper = mapper;
  }

  @Override
  public Entrega save(Entrega aggregate) {
    if (aggregate == null) {
      return null;
    }
    EntregaDocument existing =
        aggregate.getId() != null ? springDataRepo.findById(aggregate.getId()).orElse(null) : null;
    EntregaDocument saved = springDataRepo.save(mapper.toDocument(aggregate, existing));
    return mapper.toDomain(saved);
  }

  @Override
  public List<Entrega> saveAll(List<Entrega> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Entrega> findByEstado(EstadoEntrega estado) {
    if (estado == null) {
      return List.of();
    }
    return springDataRepo.findByEstado(estado).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Entrega> findByRutaId(UUID rutaId) {
    if (rutaId == null) {
      return List.of();
    }
    return springDataRepo.findByIdRutaOrderByIdAsc(rutaId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Entrega> findSinRuta() {
    return springDataRepo.findByIdRutaIsNull().stream().map(mapper::toDomain).toList();
  }

  @Override
  public boolean existsByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return false;
    }
    return springDataRepo.existsByIdDonacion(idDonacion);
  }
}
