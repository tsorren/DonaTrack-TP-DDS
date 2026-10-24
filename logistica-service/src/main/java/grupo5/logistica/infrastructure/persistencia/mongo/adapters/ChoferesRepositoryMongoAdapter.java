package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.ChoferMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataChoferMongoRepository;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataRutaMongoRepository;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.repositories.IChoferesRepository;
import java.util.List;
import java.util.UUID;
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
    super(
        springDataRepo,
        mapper::toDocument,
        doc ->
            mapper.toDomain(
                doc,
                doc != null && doc.getEstado() == EstadoChofer.EN_RUTA
                    ? rutaRepo
                        .findFirstByIdChoferAndEstado(doc.getId(), EstadoRuta.EN_TRASLADO)
                        .map(RutaDocument::getId)
                        .orElse(null)
                    : null));
    this.rutaRepo = rutaRepo;
    this.mapper = mapper;
  }

  @Override
  public List<Chofer> findActivos() {
    return springDataRepo.findByEstadoNot(EstadoChofer.DESHABILITADO).stream()
        .map(toDomain)
        .toList();
  }

  @Override
  public List<Chofer> findDisponibles() {
    return springDataRepo.findByEstado(EstadoChofer.DISPONIBLE).stream()
        .map(doc -> mapper.toDomain(doc, resolverRutaActiva(doc.getId())))
        .filter(Chofer::estaDisponibleParaAsignar)
        .toList();
  }

  private UUID resolverRutaActiva(UUID idChofer) {
    return rutaRepo
        .findFirstByIdChoferAndEstado(idChofer, EstadoRuta.EN_TRASLADO)
        .map(RutaDocument::getId)
        .orElse(null);
  }
}
