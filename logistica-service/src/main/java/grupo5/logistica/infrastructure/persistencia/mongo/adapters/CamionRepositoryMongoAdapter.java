package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static grupo5.logistica.models.entities.rutas.EstadoRuta.EN_TRASLADO;

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.CamionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataCamionMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import grupo5.logistica.models.repositories.ICamionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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
    super(springDataRepo, mapper::toDocument, conRutaSiEstaEnRuta(rutaRepo, mapper));
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
    List<CamionDocument> disponibles = springDataRepo.findByEstado(EstadoCamion.DISPONIBLE);
    List<Camion> camiones = disponibles.stream().map(this::conRutaEnTraslado).toList();
    return camiones.stream().filter(Camion::estaDisponibleParaAsignar).toList();
  }

  private Camion conRutaEnTraslado(CamionDocument doc) {
    return mapper.toDomain(doc, rutaEnTraslado(rutaRepo, doc));
  }

  /** El documento no guarda la ruta del camión: se deriva de su ruta en traslado. */
  private static Function<CamionDocument, Camion> conRutaSiEstaEnRuta(
      SpringDataRutaMongoRepository rutaRepo, CamionMongoMapper mapper) {
    return doc -> mapper.toDomain(doc, estaEnRuta(doc) ? rutaEnTraslado(rutaRepo, doc) : null);
  }

  private static boolean estaEnRuta(CamionDocument doc) {
    return doc != null && doc.getEstado() == EstadoCamion.EN_RUTA;
  }

  private static UUID rutaEnTraslado(SpringDataRutaMongoRepository rutaRepo, CamionDocument doc) {
    Optional<RutaDocument> ruta = rutaRepo.findFirstByIdCamionAndEstado(doc.getId(), EN_TRASLADO);
    return ruta.map(RutaDocument::getId).orElse(null);
  }
}
