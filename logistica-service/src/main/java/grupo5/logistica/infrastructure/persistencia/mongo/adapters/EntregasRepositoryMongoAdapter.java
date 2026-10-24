package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.AsignacionSubdocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.EntregaMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataCamionMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataEntregaMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.repositories.IEntregasRepository;
import java.util.List;
import java.util.Optional;
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
  private final SpringDataRutaMongoRepository rutaRepo;
  private final SpringDataCamionMongoRepository camionRepo;

  public EntregasRepositoryMongoAdapter(
      SpringDataEntregaMongoRepository springDataRepo,
      SpringDataRutaMongoRepository rutaRepo,
      SpringDataCamionMongoRepository camionRepo,
      EntregaMongoMapper mapper) {
    super(springDataRepo, mapper::toDocument, mapper::toDomain);
    this.mapper = mapper;
    this.rutaRepo = rutaRepo;
    this.camionRepo = camionRepo;
  }

  @Override
  public Entrega save(Entrega aggregate) {
    if (aggregate == null) {
      return null;
    }
    EntregaDocument existing =
        aggregate.getId() != null ? springDataRepo.findById(aggregate.getId()).orElse(null) : null;
    EntregaDocument doc = mapper.toDocument(aggregate, existing);
    completarAsignacionNueva(doc);
    return mapper.toDomain(springDataRepo.save(doc));
  }

  /** Copia fecha, camión y patente del día en que se asignó; una vez completa no se pisa. */
  private void completarAsignacionNueva(EntregaDocument doc) {
    AsignacionSubdocument asignacion = doc.getAsignacion();
    if (asignacion == null || asignacion.estaCompleta()) {
      return;
    }
    RutaDocument ruta =
        rutaRepo
            .findById(asignacion.getIdRuta())
            .orElseThrow(() -> new RecursoNoEncontradoException(asignacion.getIdRuta()));
    CamionDocument camion =
        camionRepo
            .findById(ruta.getIdCamion())
            .orElseThrow(() -> new RecursoNoEncontradoException(ruta.getIdCamion()));
    asignacion.completarCon(ruta, camion);
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
    return springDataRepo.findByAsignacionIdRutaOrderByIdAsc(rutaId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Entrega> findSinRuta() {
    return springDataRepo.findByAsignacionIsNull().stream().map(mapper::toDomain).toList();
  }

  @Override
  public boolean existsByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return false;
    }
    return springDataRepo.existsByIdDonacion(idDonacion);
  }

  @Override
  public Optional<Entrega> findByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return Optional.empty();
    }
    return springDataRepo.findByIdDonacion(idDonacion).map(mapper::toDomain);
  }

  @Override
  public List<Entrega> findByIdBeneficiaria(UUID idBeneficiaria) {
    if (idBeneficiaria == null) {
      return List.of();
    }
    return springDataRepo.findByIdBeneficiaria(idBeneficiaria).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
