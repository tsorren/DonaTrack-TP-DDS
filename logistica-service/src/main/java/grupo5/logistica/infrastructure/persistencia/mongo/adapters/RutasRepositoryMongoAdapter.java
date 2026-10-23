package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.ParadaSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.RutaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataEntregaMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.IRutasRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class RutasRepositoryMongoAdapter
    extends CrudRepositoryMongoAdapter<Ruta, RutaDocument, SpringDataRutaMongoRepository>
    implements IRutasRepository {

  private final RutaMongoMapper mapper;
  private final SpringDataEntregaMongoRepository entregaRepo;

  public RutasRepositoryMongoAdapter(
      SpringDataRutaMongoRepository springDataRepo,
      SpringDataEntregaMongoRepository entregaRepo,
      RutaMongoMapper mapper) {
    super(springDataRepo, mapper::toDocument, mapper::toDomain);
    this.mapper = mapper;
    this.entregaRepo = entregaRepo;
  }

  @Override
  public Ruta save(Ruta aggregate) {
    if (aggregate == null) {
      return null;
    }
    RutaDocument existing =
        aggregate.getId() != null ? springDataRepo.findById(aggregate.getId()).orElse(null) : null;
    RutaDocument doc = mapper.toDocument(aggregate, existing);
    doc.getParadas().stream().filter(p -> p.getIdDonacion() == null).forEach(this::completarParada);
    return mapper.toDomain(springDataRepo.save(doc));
  }

  private void completarParada(ParadaSubdocument parada) {
    EntregaDocument entrega =
        entregaRepo
            .findById(parada.getIdEntrega())
            .orElseThrow(() -> new RecursoNoEncontradoException(parada.getIdEntrega()));
    parada.setIdDonacion(entrega.getIdDonacion());
    parada.setDestino(entrega.getDestino());
  }

  @Override
  public List<Ruta> saveAll(List<Ruta> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Ruta> findByFecha(LocalDate fecha) {
    if (fecha == null) {
      return List.of();
    }
    return springDataRepo.findByFecha(fecha).stream().map(toDomain).toList();
  }

  @Override
  public List<Ruta> findByCamionId(UUID camionId) {
    if (camionId == null) {
      return List.of();
    }
    return springDataRepo.findByIdCamion(camionId).stream().map(toDomain).toList();
  }

  @Override
  public List<Ruta> findByCamionIdAndFecha(UUID camionId, LocalDate fecha) {
    if (camionId == null || fecha == null) {
      return List.of();
    }
    return springDataRepo.findByIdCamionAndFecha(camionId, fecha).stream().map(toDomain).toList();
  }
}
