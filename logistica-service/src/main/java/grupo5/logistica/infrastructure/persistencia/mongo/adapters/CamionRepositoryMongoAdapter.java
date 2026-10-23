package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.CamionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataCamionMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.repositories.ICamionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class CamionRepositoryMongoAdapter
    extends CrudRepositoryMongoAdapter<Camion, CamionDocument, SpringDataCamionMongoRepository>
    implements ICamionRepository {

  private final SpringDataRutaMongoRepository rutaRepo;
  private final CamionMongoMapper mapper;

  public CamionRepositoryMongoAdapter(
      SpringDataCamionMongoRepository springDataRepo,
      SpringDataRutaMongoRepository rutaRepo,
      CamionMongoMapper mapper) {
    super(
        springDataRepo,
        mapper::toDocument,
        doc ->
            mapper.toDomain(
                doc,
                doc != null && doc.getEstado() == EstadoCamion.EN_RUTA
                    ? rutaRepo
                        .findFirstByIdCamionAndEstado(doc.getId(), EstadoRuta.EN_TRASLADO)
                        .map(RutaDocument::getId)
                        .orElse(null)
                    : null));
    this.rutaRepo = rutaRepo;
    this.mapper = mapper;
  }

  @Override
  public Camion save(Camion aggregate) {
    if (aggregate == null) {
      return null;
    }
    CamionDocument existing =
        aggregate.getId() != null ? springDataRepo.findById(aggregate.getId()).orElse(null) : null;
    CamionDocument saved = springDataRepo.save(mapper.toDocument(aggregate, existing));
    return toDomain.apply(saved);
  }

  @Override
  public List<Camion> saveAll(List<Camion> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Camion> findByEstado(EstadoCamion estado) {
    if (estado == null) {
      return List.of();
    }
    return springDataRepo.findByEstado(estado).stream().map(toDomain).toList();
  }

  @Override
  public Optional<Camion> findByPatente(String patente) {
    if (patente == null || patente.isBlank()) {
      return Optional.empty();
    }
    String patenteNormalizada = ValidadorPatentes.normalizar(patente);
    return springDataRepo.findByPatenteIgnoreCase(patenteNormalizada).map(toDomain);
  }

  @Override
  public List<Camion> findActivos() {
    return springDataRepo.findByEstadoNot(EstadoCamion.DESHABILITADO).stream()
        .map(toDomain)
        .toList();
  }

  @Override
  public List<Camion> findDisponibles() {
    return springDataRepo.findByEstado(EstadoCamion.DISPONIBLE).stream()
        .map(doc -> mapper.toDomain(doc, resolverRutaActiva(doc.getId())))
        .filter(Camion::estaDisponibleParaAsignar)
        .toList();
  }

  private UUID resolverRutaActiva(UUID idCamion) {
    return rutaRepo
        .findFirstByIdCamionAndEstado(idCamion, EstadoRuta.EN_TRASLADO)
        .map(RutaDocument::getId)
        .orElse(null);
  }
}
