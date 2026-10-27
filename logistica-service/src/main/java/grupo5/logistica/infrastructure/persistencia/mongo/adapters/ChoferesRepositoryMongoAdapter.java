package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static grupo5.logistica.models.entities.rutas.EstadoRuta.EN_TRASLADO;

import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.ChoferMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataChoferMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.repositories.IChoferesRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class ChoferesRepositoryMongoAdapter
    extends CrudRepositoryMongoAdapter<Chofer, ChoferDocument, SpringDataChoferMongoRepository>
    implements IChoferesRepository {

  private final SpringDataRutaMongoRepository rutaRepo;
  private final ChoferMongoMapper mapper;

  public ChoferesRepositoryMongoAdapter(
      SpringDataChoferMongoRepository springDataRepo,
      SpringDataRutaMongoRepository rutaRepo,
      ChoferMongoMapper mapper) {
    super(springDataRepo, mapper::toDocument, conRutaSiEstaEnRuta(rutaRepo, mapper));
    this.rutaRepo = rutaRepo;
    this.mapper = mapper;
  }

  @Override
  public List<Chofer> findActivos() {
    return aDominio(springDataRepo.findByEstadoNot(EstadoChofer.DESHABILITADO));
  }

  @Override
  public List<Chofer> findDisponibles() {
    List<ChoferDocument> disponibles = springDataRepo.findByEstado(EstadoChofer.DISPONIBLE);
    List<Chofer> choferes = disponibles.stream().map(this::conRutaEnTraslado).toList();
    return choferes.stream().filter(Chofer::estaDisponibleParaAsignar).toList();
  }

  private Chofer conRutaEnTraslado(ChoferDocument doc) {
    return mapper.toDomain(doc, rutaEnTraslado(rutaRepo, doc));
  }

  /** El documento no guarda la ruta del chofer: se deriva de su ruta en traslado. */
  private static Function<ChoferDocument, Chofer> conRutaSiEstaEnRuta(
      SpringDataRutaMongoRepository rutaRepo, ChoferMongoMapper mapper) {
    return doc -> mapper.toDomain(doc, estaEnRuta(doc) ? rutaEnTraslado(rutaRepo, doc) : null);
  }

  private static boolean estaEnRuta(ChoferDocument doc) {
    return doc != null && doc.getEstado() == EstadoChofer.EN_RUTA;
  }

  private static UUID rutaEnTraslado(SpringDataRutaMongoRepository rutaRepo, ChoferDocument doc) {
    Optional<RutaDocument> ruta = rutaRepo.findFirstByIdChoferAndEstado(doc.getId(), EN_TRASLADO);
    return ruta.map(RutaDocument::getId).orElse(null);
  }
}
