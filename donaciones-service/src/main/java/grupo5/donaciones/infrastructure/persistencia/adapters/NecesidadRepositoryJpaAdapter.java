package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadRecurrenteEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataNecesidadRepository;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioNecesidades;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("postgres")
public class NecesidadRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Necesidad, NecesidadEntity, SpringDataNecesidadRepository>
    implements INecesidadesRepository {

  /**
   * Lo asignado a la necesidad {@code n} en el período que elige la condición {@code %1$s}: la suma
   * de los ítems de sus donaciones menos lo que no llegó, como {@code
   * DonacionIndependienteRepositoryJpaAdapter}. Las dos sumas van por separado para no contar lo no
   * recibido una vez por ítem.
   */
  private static final String ASIGNADO =
      "((SELECT coalesce(sum(i.cantidad), 0) FROM donaciones.donacion_independiente d"
          + " JOIN donaciones.item_donacion_independiente i ON i.id_donacion_independiente = d.id"
          + " WHERE d.id_necesidad = n.id AND %1$s)"
          + " - (SELECT coalesce(sum(d.cantidad_no_recibida), 0)"
          + " FROM donaciones.donacion_independiente d"
          + " WHERE d.id_necesidad = n.id AND %1$s))";

  /**
   * Satisfecha (SPEC-11, G2): la extraordinaria, con lo asignado sin período; la recurrente, con lo
   * asignado a su período actual (el de fecha de fin más lejana, el último que lee el dominio)
   * contra el objetivo de ese período. Una recurrente sin períodos no está satisfecha.
   */
  private static final String SATISFECHA =
      "((n.tipo_necesidad = 'EXTRAORDINARIA' AND "
          + ASIGNADO.formatted("d.id_periodo IS NULL")
          + " >= n.cantidad_necesitada)"
          + " OR (n.tipo_necesidad = 'RECURRENTE' AND EXISTS (SELECT 1"
          + " FROM donaciones.periodo_necesidad p WHERE p.necesidad_id = n.id"
          + " AND p.fecha_fin = (SELECT max(pa.fecha_fin) FROM donaciones.periodo_necesidad pa"
          + " WHERE pa.necesidad_id = n.id)"
          + " AND "
          + ASIGNADO.formatted("d.id_periodo = p.id")
          + " >= p.cantidad_objetivo)))";

  private static final String DE_LA_CATEGORIA =
      "EXISTS (SELECT 1 FROM donaciones.subcategoria s"
          + " WHERE s.id = n.subcategoria_id AND s.id_categoria = :categoriaId)";

  @PersistenceContext private EntityManager em;

  private final NecesidadesPersistenciaMapper mapper;
  private final TransactionTemplate transacciones;

  public NecesidadRepositoryJpaAdapter(
      SpringDataNecesidadRepository springDataRepo,
      NecesidadesPersistenciaMapper mapper,
      PlatformTransactionManager transactionManager) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
    this.transacciones = new TransactionTemplate(transactionManager);
  }

  /** Una consulta para todas (la página de propuestas, R11-24). */
  @Override
  @Transactional(readOnly = true)
  public List<Necesidad> findAllById(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(ids).stream().map(toDomain).toList();
  }

  /**
   * Guarda con bloqueo optimista: si la fila cambió desde que se leyó la necesidad, falla con
   * {@code NECESIDAD_MODIFICADA_CONCURRENTEMENTE} en vez de pisar el otro guardado. La comparación
   * explícita cubre el caso en que el dominio se leyó en otra operación; {@code @Version} cubre la
   * carrera entre esta lectura y el guardado.
   *
   * <p>Lee y guarda en una sola transacción, forzando el incremento de la versión: sin eso, renovar
   * el período de una recurrente (que solo agrega una fila hija) no cambiaría la versión y dos
   * renovaciones concurrentes duplicarían el período. Hibernate aplica ese incremento al confirmar,
   * por eso la versión nueva se lee recién después del commit.
   */
  @Override
  public Necesidad save(Necesidad domainEntity) {
    NecesidadEntity savedEntity;
    try {
      savedEntity = transacciones.execute(estado -> guardarForzandoVersion(domainEntity));
    } catch (OptimisticLockingFailureException e) {
      throw new BusinessStateException(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE);
    }
    // El que llamó sigue usando su objeto: queda con la versión nueva para el próximo guardado
    domainEntity.asignarVersion(savedEntity.getVersion());
    return this.mapper.toDomain(savedEntity);
  }

  private NecesidadEntity guardarForzandoVersion(Necesidad domainEntity) {
    NecesidadEntity existing =
        this.springDataRepo.findByIdForzandoVersion(domainEntity.getId()).orElse(null);
    if (existing != null && !Objects.equals(existing.getVersion(), domainEntity.getVersion())) {
      throw new BusinessStateException(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE);
    }
    return this.springDataRepo.saveAndFlush(this.mapper.toEntity(domainEntity, existing));
  }

  @Override
  public List<Necesidad> saveAll(List<Necesidad> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Necesidad> findActivas() {
    return springDataRepo.findByActiva(true).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId) {
    if (entidadId == null) {
      return List.of();
    }
    return this.springDataRepo.findByEntidadBeneficiariaId(entidadId).stream()
        .map(this.mapper::toDomain)
        .toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndRecurrenteTrue() {
    return springDataRepo.findActivasDelTipo(NecesidadRecurrenteEntity.class).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public boolean existsBySubcategoriaId(UUID subcategoriaId) {
    return subcategoriaId != null && springDataRepo.existsBySubcategoriaId(subcategoriaId);
  }

  /** SPEC-11, G2: filtros, "satisfecha", orden y página en SQL, como el listado de entidades. */
  @Override
  @Transactional(readOnly = true)
  public Tramo<Necesidad> buscarVigentes(CriterioNecesidades criterio) {
    ConsultaListado consulta =
        new ConsultaListado("FROM donaciones.necesidad n")
            .donde("n.activa")
            .donde("(n.fecha_limite IS NULL OR n.fecha_limite >= :hoy)", "hoy", criterio.hoy())
            .siHay(criterio.entidadId(), "n.entidad_beneficiaria_id = :entidadId", "entidadId")
            .siHay(
                criterio.subcategoriaId(), "n.subcategoria_id = :subcategoriaId", "subcategoriaId")
            .siHay(criterio.categoriaId(), DE_LA_CATEGORIA, "categoriaId")
            .siHay(
                criterio.tipo() == null ? null : criterio.tipo().name(),
                "n.tipo_necesidad = :tipo",
                "tipo")
            .donde("NOT " + SATISFECHA);
    List<UUID> ids =
        consulta.ids(
            em, "n.id", ConsultaListado.sinTildes("n.descripcion") + ", n.id", criterio.paginado());
    List<Necesidad> pagina =
        ConsultaListado.enOrden(
            ids, springDataRepo.findAllById(ids).stream().map(toDomain).toList(), Necesidad::getId);
    return new Tramo<>(pagina, consulta.total(em));
  }
}
