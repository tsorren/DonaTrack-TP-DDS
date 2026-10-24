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
                    ? rutaEnTraslado(rutaRepo, doc.getId())
                    : null));
    this.rutaRepo = rutaRepo;
    this.mapper = mapper;
  }

  @Override
  public List<Camion> findByEstado(EstadoCamion estado) {
    if (estado == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByEstado(estado));
  }

  @Override
  public Optional<Camion> findByPatente(String patente) {
    if (patente == null || patente.isBlank()) {
      return Optional.empty();
    }
    String patenteNormalizada = ValidadorPatentes.normalizar(patente);
    return springDataRepo.findByPatente(patenteNormalizada).map(toDomain);
  }

  @Override
  public List<Camion> findActivos() {
    return aDominio(springDataRepo.findByEstadoNot(EstadoCamion.DESHABILITADO));
  }

  @Override
  public List<Camion> findDisponibles() {
    return springDataRepo.findByEstado(EstadoCamion.DISPONIBLE).stream()
        .map(doc -> mapper.toDomain(doc, rutaEnTraslado(rutaRepo, doc.getId())))
        .filter(Camion::estaDisponibleParaAsignar)
        .toList();
  }

  private static UUID rutaEnTraslado(SpringDataRutaMongoRepository rutaRepo, UUID idCamion) {
    return rutaRepo
        .findFirstByIdCamionAndEstado(idCamion, EstadoRuta.EN_TRASLADO)
        .map(RutaDocument::getId)
        .orElse(null);
  }
}
