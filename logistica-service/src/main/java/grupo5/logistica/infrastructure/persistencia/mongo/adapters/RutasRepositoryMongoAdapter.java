package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

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
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class RutasRepositoryMongoAdapter
    extends CrudRepositoryMongoAdapter<Ruta, RutaDocument, SpringDataRutaMongoRepository>
    implements IRutasRepository {

  private final SpringDataEntregaMongoRepository entregaRepo;

  public RutasRepositoryMongoAdapter(
      SpringDataRutaMongoRepository springDataRepo,
      SpringDataEntregaMongoRepository entregaRepo,
      RutaMongoMapper mapper) {
    super(springDataRepo, mapper::toDocument, mapper::toDomain);
    this.entregaRepo = entregaRepo;
  }

  /** Copia donación y destino de las entregas que se agregaron, leyéndolas en una sola consulta. */
  @Override
  protected void completarAntesDeGuardar(RutaDocument doc) {
    List<ParadaSubdocument> nuevas = sinCompletar(doc.getParadas());
    if (nuevas.isEmpty()) {
      return;
    }
    List<UUID> idsEntregas = nuevas.stream().map(ParadaSubdocument::getIdEntrega).toList();
    Map<UUID, EntregaDocument> entregasPorId = porId(entregaRepo.findAllById(idsEntregas));
    nuevas.forEach(parada -> parada.completarCon(entregaDe(parada, entregasPorId)));
  }

  private static List<ParadaSubdocument> sinCompletar(List<ParadaSubdocument> paradas) {
    return paradas.stream().filter(Predicate.not(ParadaSubdocument::estaCompleta)).toList();
  }

  private static Map<UUID, EntregaDocument> porId(List<EntregaDocument> entregas) {
    return entregas.stream().collect(toMap(EntregaDocument::getId, identity()));
  }

  private static EntregaDocument entregaDe(
      ParadaSubdocument parada, Map<UUID, EntregaDocument> entregasPorId) {
    EntregaDocument entrega = entregasPorId.get(parada.getIdEntrega());
    if (entrega == null) {
      throw new RecursoNoEncontradoException(parada.getIdEntrega());
    }
    return entrega;
  }

  @Override
  public List<Ruta> findByFecha(LocalDate fecha) {
    if (fecha == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByFecha(fecha));
  }

  @Override
  public List<Ruta> findByCamionId(UUID camionId) {
    if (camionId == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByIdCamion(camionId));
  }

  @Override
  public List<Ruta> findByCamionIdAndFecha(UUID camionId, LocalDate fecha) {
    if (camionId == null || fecha == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByIdCamionAndFecha(camionId, fecha));
  }
}
