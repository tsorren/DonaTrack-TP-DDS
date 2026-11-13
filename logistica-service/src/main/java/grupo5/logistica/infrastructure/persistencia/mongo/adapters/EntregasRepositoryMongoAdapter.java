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
import grupo5.logistica.models.repositories.CriterioSeguimiento;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.TramoDeEntregas;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DataTypeOperators;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class EntregasRepositoryMongoAdapter
    extends CrudRepositoryMongoAdapter<Entrega, EntregaDocument, SpringDataEntregaMongoRepository>
    implements IEntregasRepository {

  private final SpringDataRutaMongoRepository rutaRepo;
  private final SpringDataCamionMongoRepository camionRepo;
  private final MongoTemplate mongoTemplate;

  public EntregasRepositoryMongoAdapter(
      SpringDataEntregaMongoRepository springDataRepo,
      SpringDataRutaMongoRepository rutaRepo,
      SpringDataCamionMongoRepository camionRepo,
      EntregaMongoMapper mapper) {
    this(springDataRepo, rutaRepo, camionRepo, mapper, null);
  }

  /** {@code mongoTemplate}: el seguimiento paginado (R11-43); sin él, se ordena en memoria. */
  @Autowired
  public EntregasRepositoryMongoAdapter(
      SpringDataEntregaMongoRepository springDataRepo,
      SpringDataRutaMongoRepository rutaRepo,
      SpringDataCamionMongoRepository camionRepo,
      EntregaMongoMapper mapper,
      @Nullable MongoTemplate mongoTemplate) {
    super(springDataRepo, mapper::toDocument, mapper::toDomain);
    this.rutaRepo = rutaRepo;
    this.camionRepo = camionRepo;
    this.mongoTemplate = mongoTemplate;
  }

  /** Copia fecha, camión y patente del día en que se asignó; una vez completa no se pisa. */
  @Override
  protected void completarAntesDeGuardar(EntregaDocument doc) {
    AsignacionSubdocument asignacion = doc.getAsignacion();
    if (asignacion == null || asignacion.estaCompleta()) {
      return;
    }
    RutaDocument ruta = buscarExistente(rutaRepo, asignacion.getIdRuta());
    CamionDocument camion = buscarExistente(camionRepo, ruta.getIdCamion());
    asignacion.completarCon(ruta, camion);
  }

  private static <D> D buscarExistente(MongoRepository<D, UUID> repo, UUID id) {
    return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  @Override
  public List<Entrega> findByEstado(EstadoEntrega estado) {
    if (estado == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByEstado(estado));
  }

  @Override
  public List<Entrega> findEntregadasParaMapa(Instant desde, Instant hasta, int limite) {
    if (desde == null || hasta == null || limite <= 0) {
      return List.of();
    }
    Sort orden = Sort.by(Sort.Order.desc("horaArribo"), Sort.Order.asc("id"));
    return aDominio(
        springDataRepo.buscarParaMapa(
            EstadoEntrega.ENTREGADA, desde, hasta, PageRequest.of(0, limite, orden)));
  }

  @Override
  public long contarEntregadasParaMapa(Instant desde, Instant hasta) {
    if (desde == null || hasta == null) {
      return 0;
    }
    return springDataRepo.contarParaMapa(EstadoEntrega.ENTREGADA, desde, hasta);
  }

  @Override
  public List<Entrega> findByRutaId(UUID rutaId) {
    if (rutaId == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByAsignacionIdRutaOrderByIdAsc(rutaId));
  }

  @Override
  public List<Entrega> findSinRuta() {
    return aDominio(springDataRepo.findByAsignacionIsNull());
  }

  @Override
  public boolean existsByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return false;
    }
    return springDataRepo.existsByIdDonacionAndEstadoNot(idDonacion, EstadoEntrega.CANCELADA);
  }

  @Override
  public Optional<Entrega> findByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return Optional.empty();
    }
    return springDataRepo
        .findFirstByIdDonacionAndEstadoNot(idDonacion, EstadoEntrega.CANCELADA)
        .map(toDomain);
  }

  @Override
  public List<Entrega> findCanceladasByIdDonacion(UUID idDonacion) {
    return springDataRepo.findByIdDonacionAndEstado(idDonacion, EstadoEntrega.CANCELADA).stream()
        .map(toDomain)
        .toList();
  }

  @Override
  public List<Entrega> findByIdBeneficiaria(UUID idBeneficiaria) {
    if (idBeneficiaria == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByIdBeneficiaria(idBeneficiaria));
  }

  @Override
  public List<Entrega> findByDonanteId(UUID donanteId) {
    if (donanteId == null) {
      return List.of();
    }
    return aDominio(springDataRepo.findByDonanteId(donanteId));
  }

  /**
   * El seguimiento en la base (R11-43): un {@code $match} con los filtros presentes, el orden de
   * {@link CriterioSeguimiento#ORDEN} (los campos calculados "llego" y "salio" ponen primero las
   * que no tienen esa hora, como el NULLS FIRST de SQL) y el tramo pedido; el total, con un conteo
   * de los mismos filtros.
   */
  @Override
  public TramoDeEntregas buscarSeguimiento(CriterioSeguimiento criterio) {
    if (mongoTemplate == null) {
      return TramoDeEntregas.de(
          findAll().stream().filter(criterio::cumple).sorted(CriterioSeguimiento.ORDEN).toList(),
          criterio);
    }
    Criteria filtro = filtroDeSeguimiento(criterio);
    TypedAggregation<EntregaDocument> consulta =
        Aggregation.newAggregation(
            EntregaDocument.class,
            Aggregation.match(filtro),
            Aggregation.addFields()
                .addFieldWithValue(
                    "llego",
                    ConditionalOperators.when(
                            ComparisonOperators.valueOf(DataTypeOperators.Type.typeOf("horaArribo"))
                                .equalToValue("date"))
                        .then(1)
                        .otherwise(0))
                .addFieldWithValue(
                    "salio",
                    ConditionalOperators.when(
                            ComparisonOperators.valueOf(DataTypeOperators.Type.typeOf("horaSalida"))
                                .equalToValue("date"))
                        .then(1)
                        .otherwise(0))
                .build(),
            Aggregation.sort(
                Sort.by(
                    Sort.Order.asc("llego"),
                    Sort.Order.desc("horaArribo"),
                    Sort.Order.asc("salio"),
                    Sort.Order.desc("horaSalida"),
                    Sort.Order.asc("id"))),
            Aggregation.skip(criterio.desplazamiento()),
            Aggregation.limit(criterio.tamanio()));
    List<EntregaDocument> pagina =
        mongoTemplate.aggregate(consulta, EntregaDocument.class).getMappedResults();
    long total = mongoTemplate.count(new Query(filtro), EntregaDocument.class);
    return new TramoDeEntregas(aDominio(pagina), total);
  }

  private static Criteria filtroDeSeguimiento(CriterioSeguimiento criterio) {
    Criteria filtro = new Criteria();
    if (criterio.idBeneficiaria() != null) {
      filtro = filtro.and("idBeneficiaria").is(criterio.idBeneficiaria());
    }
    if (criterio.donanteId() != null) {
      filtro = filtro.and("donanteId").is(criterio.donanteId());
    }
    if (!criterio.estados().isEmpty()) {
      filtro = filtro.and("estado").in(criterio.estados());
    }
    if (!criterio.idsDonacion().isEmpty()) {
      filtro = filtro.and("idDonacion").in(criterio.idsDonacion());
    }
    return filtro;
  }

  @Override
  public List<Entrega> findByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return aDominio(springDataRepo.findAllById(Set.copyOf(ids)));
  }
}
