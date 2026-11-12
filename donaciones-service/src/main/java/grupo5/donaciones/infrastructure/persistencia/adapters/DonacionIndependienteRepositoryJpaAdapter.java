package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.DonacionIndependienteEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionIndependienteRepository;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionIndependienteRepository.ConteoPorEntidad;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionIndependienteRepository.SumaPorDestino;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.listados.ConteoAsignadas;
import grupo5.donaciones.models.repositories.listados.CriterioDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.OrdenDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.PorVencer;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class DonacionIndependienteRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        DonacionIndependiente,
        DonacionIndependienteEntity,
        SpringDataDonacionIndependienteRepository>
    implements IDonacionesIndependientesRepository {

  // Filtros del listado (R11-01): lo que mira otras tablas va en un EXISTS
  /**
   * R11-43: el grupo de cada estado en el orden de lo que hay que atender (el mismo que {@link
   * OrdenDonacionesIndependientes#grupoDeAtencion}).
   */
  private static final String ATENCION =
      "CASE di.estado_actual WHEN 'EN_PUERTA' THEN 0"
          + " WHEN 'ENTREGA_FALLIDA' THEN 1 WHEN 'VENCIDA' THEN 1"
          + " WHEN 'EN_TRASLADO' THEN 2"
          + " WHEN 'ASIGNACION_REALIZADA' THEN 3 WHEN 'LISTA_PARA_ENTREGAR' THEN 3"
          + " WHEN 'EN_DEPOSITO' THEN 3 WHEN 'ENTREGADA' THEN 4 ELSE 5 END";

  private static final String DEL_DONANTE =
      "EXISTS (SELECT 1 FROM donaciones.donacion dn WHERE dn.id = di.id_donacion"
          + " AND dn.id_donante = :donanteId)";

  private static final String DE_LA_SUBCATEGORIA =
      "EXISTS (SELECT 1 FROM donaciones.item_donacion_independiente i"
          + " JOIN donaciones.item_donacion_normalizado n ON n.id = i.id_item_normalizado"
          + " WHERE i.id_donacion_independiente = di.id AND n.id_subcategoria = :subcategoriaId)";

  // R11-42: las series del inicio del admin, contadas con GROUP BY (ninguna lista se trae)
  private static final String PASARON_A =
      " FROM donaciones.cambio_estado_donacion_independiente c"
          + " JOIN donaciones.estado_donacion_independiente e ON e.id = c.id_estado_nuevo";
  private static final String EN_EL_TRAMO_DEL_CAMBIO =
      " e.tipo = :estado AND c.\"timestamp\" >= :desde AND c.\"timestamp\" < :hasta";
  private static final String CATEGORIA_DE_LOS_ITEMS =
      " JOIN donaciones.item_donacion_normalizado n ON n.id = i.id_item_normalizado"
          + " JOIN donaciones.subcategoria s ON s.id = n.id_subcategoria";

  private static final String DE_LA_CATEGORIA =
      "EXISTS (SELECT 1 FROM donaciones.item_donacion_independiente ic"
          + " JOIN donaciones.item_donacion_normalizado nc ON nc.id = ic.id_item_normalizado"
          + " JOIN donaciones.subcategoria s ON s.id = nc.id_subcategoria"
          + " WHERE ic.id_donacion_independiente = di.id AND s.id_categoria = :categoriaId)";

  // R11-34: el vencimiento más próximo de sus bienes (nulo si ninguno vence)
  private static final String PRIMER_VENCIMIENTO =
      "(SELECT min(nv.fecha_vencimiento) FROM donaciones.item_donacion_independiente iv"
          + " JOIN donaciones.item_donacion_normalizado nv ON nv.id = iv.id_item_normalizado"
          + " WHERE iv.id_donacion_independiente = di.id)";

  private static final String VENCE_A_MAS_TARDAR =
      "EXISTS (SELECT 1 FROM donaciones.item_donacion_independiente ih"
          + " JOIN donaciones.item_donacion_normalizado nh ON nh.id = ih.id_item_normalizado"
          + " WHERE ih.id_donacion_independiente = di.id AND nh.fecha_vencimiento <= :venceHasta)";

  // En depósito con algún bien que vence en el rango (resumen del admin)
  private static final String POR_VENCER =
      " FROM donaciones.donacion_independiente di"
          + " JOIN donaciones.item_donacion_independiente i ON i.id_donacion_independiente = di.id"
          + " JOIN donaciones.item_donacion_normalizado n ON n.id = i.id_item_normalizado"
          + " WHERE di.estado_actual = 'EN_DEPOSITO'"
          + " AND n.fecha_vencimiento BETWEEN :desde AND :hasta";

  private final DonacionesIndependientesPersistenciaMapper mapper;

  @PersistenceContext private EntityManager em;

  public DonacionIndependienteRepositoryJpaAdapter(
      SpringDataDonacionIndependienteRepository springDataRepo,
      DonacionesIndependientesPersistenciaMapper mapper) {
    super(springDataRepo, d -> mapper.toEntity(d, null), mapper::toDomain);
    this.mapper = mapper;
  }

  /**
   * Parte de la fila guardada para agregar al historial solo los cambios nuevos. Bloqueo optimista:
   * si la fila cambió desde que se leyó la donación, falla con {@code
   * DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE} en vez de pisar el otro guardado. La
   * versión sube en cada guardado, también cuando solo cambian los ítems (A2).
   */
  @Override
  @Transactional
  public DonacionIndependiente save(DonacionIndependiente donacion) {
    DonacionIndependienteEntity existente = springDataRepo.findById(donacion.getId()).orElse(null);
    if (existente != null) {
      // Si no matchea ninguna fila, otra operación la guardó después de que se leyó la donación
      if (springDataRepo.subirVersion(donacion.getId(), donacion.getVersion()) == 0) {
        throw new BusinessStateException(
            ErrorCatalog.DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE);
      }
      // La consulta limpió el contexto: se vuelve a leer la fila con la versión nueva
      existente = springDataRepo.findById(donacion.getId()).orElseThrow();
    }
    DonacionIndependienteEntity guardada =
        springDataRepo.saveAndFlush(mapper.toEntity(donacion, existente));
    // El que llamó sigue usando su objeto: queda con la versión nueva para el próximo guardado
    donacion.asignarVersion(guardada.getVersion());
    return mapper.toDomain(guardada);
  }

  @Override
  @Transactional
  public List<DonacionIndependiente> saveAll(List<DonacionIndependiente> donaciones) {
    return donaciones.stream().map(this::save).toList();
  }

  // Ítems, estados e historial son colecciones lazy: se mapean a dominio con la sesión abierta
  @Override
  @Transactional(readOnly = true)
  public Optional<DonacionIndependiente> findById(UUID id) {
    return super.findById(id);
  }

  /** Una consulta para todas (la página de propuestas, R11-24). */
  @Override
  @Transactional(readOnly = true)
  public List<DonacionIndependiente> findAllById(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(ids).stream().map(mapper::toDomain).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DonacionIndependiente> findAll() {
    return super.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DonacionIndependiente> findEnDeposito() {
    return springDataRepo.findByEstadoActual(TipoEstadoDonacion.EN_DEPOSITO).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DonacionIndependiente> findCancelablesPorNecesidad(UUID necesidadId) {
    return springDataRepo
        .findByNecesidadIdAndEstadoActualIn(
            necesidadId,
            List.of(
                TipoEstadoDonacion.ASIGNACION_REALIZADA, TipoEstadoDonacion.LISTA_PARA_ENTREGAR))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public int sumarCantidadAsignada(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    long suma =
        periodoId != null
            ? springDataRepo.sumarCantidadAsignadaEnPeriodo(necesidadId, periodoId)
                - springDataRepo.sumarNoRecibidoEnPeriodo(necesidadId, periodoId)
            : springDataRepo.sumarCantidadAsignadaSinPeriodo(necesidadId)
                - springDataRepo.sumarNoRecibidoSinPeriodo(necesidadId);
    return Math.toIntExact(suma);
  }

  @Override
  public Map<UUID, Integer> sumarCantidadAsignada(Map<UUID, UUID> periodoPorNecesidad) {
    if (periodoPorNecesidad.isEmpty()) {
      return new HashMap<>();
    }
    return delPeriodoActual(
        periodoPorNecesidad,
        springDataRepo.sumarCantidadAsignadaPorDestino(periodoPorNecesidad.keySet()));
  }

  @Override
  public int sumarCantidadRecibida(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    long suma =
        periodoId != null
            ? springDataRepo.sumarCantidadEnPeriodoConEstado(
                necesidadId, periodoId, TipoEstadoDonacion.ENTREGADA)
            : springDataRepo.sumarCantidadSinPeriodoConEstado(
                necesidadId, TipoEstadoDonacion.ENTREGADA);
    return Math.toIntExact(suma);
  }

  @Override
  public Map<UUID, Integer> sumarCantidadRecibida(Map<UUID, UUID> periodoPorNecesidad) {
    if (periodoPorNecesidad.isEmpty()) {
      return new HashMap<>();
    }
    return delPeriodoActual(
        periodoPorNecesidad,
        springDataRepo.sumarCantidadPorDestinoConEstado(
            periodoPorNecesidad.keySet(), TipoEstadoDonacion.ENTREGADA));
  }

  private Map<UUID, Integer> delPeriodoActual(
      Map<UUID, UUID> periodoPorNecesidad, List<SumaPorDestino> filas) {
    Map<UUID, Integer> sumas = new HashMap<>();
    periodoPorNecesidad.keySet().forEach(id -> sumas.put(id, 0));
    for (SumaPorDestino fila : filas) {
      // Solo cuenta el período actual de la necesidad: los anteriores ya se cerraron
      if (Objects.equals(fila.getPeriodoId(), periodoPorNecesidad.get(fila.getNecesidadId()))) {
        sumas.put(fila.getNecesidadId(), Math.toIntExact(fila.getCantidad()));
      }
    }
    for (SumaPorDestino fila :
        springDataRepo.sumarNoRecibidoPorDestino(periodoPorNecesidad.keySet())) {
      if (Objects.equals(fila.getPeriodoId(), periodoPorNecesidad.get(fila.getNecesidadId()))) {
        sumas.merge(fila.getNecesidadId(), -Math.toIntExact(fila.getCantidad()), Integer::sum);
      }
    }
    return sumas;
  }

  @Override
  public Map<UUID, Integer> contarPorEntidadRegistradasDesde(LocalDateTime desde) {
    List<ConteoPorEntidad> filas =
        desde != null
            ? springDataRepo.contarPorEntidadRegistradasDesde(desde)
            : springDataRepo.contarPorEntidad();
    Map<UUID, Integer> conteo = new HashMap<>();
    filas.forEach(f -> conteo.put(f.getEntidadId(), Math.toIntExact(f.getCantidad())));
    return conteo;
  }

  @Override
  public long contarEnEstado(TipoEstadoDonacion estado) {
    return springDataRepo.countByEstadoActual(estado);
  }

  @Override
  @Transactional(readOnly = true)
  public Tramo<DonacionIndependiente> buscar(CriterioDonacionesIndependientes c) {
    ConsultaListado consulta =
        new ConsultaListado("FROM donaciones.donacion_independiente di")
            .siHay(
                c.estados().stream().map(TipoEstadoDonacion::name).toList(),
                "di.estado_actual IN (:estados)",
                "estados")
            .siHay(c.donanteId(), DEL_DONANTE, "donanteId")
            .siHay(c.subcategoriaId(), DE_LA_SUBCATEGORIA, "subcategoriaId")
            .siHay(c.categoriaId(), DE_LA_CATEGORIA, "categoriaId")
            .siHay(c.entidadId(), "di.id_beneficiario_asignado = :entidadId", "entidadId")
            .siHay(c.necesidadId(), "di.id_necesidad = :necesidadId", "necesidadId")
            .siHay(c.id(), "di.id = :id", "id")
            .siHay(c.venceHasta(), VENCE_A_MAS_TARDAR, "venceHasta")
            .siHay(
                c.desde() == null ? null : c.desde().atStartOfDay(),
                "di.fecha_registro >= :desde",
                "desde")
            .siHay(
                c.hasta() == null ? null : c.hasta().plusDays(1).atStartOfDay(),
                "di.fecha_registro < :hasta",
                "hasta");
    if (c.q() != null) {
      consulta.donde(
          "EXISTS (SELECT 1 FROM donaciones.item_donacion_independiente iq"
              + " WHERE iq.id_donacion_independiente = di.id AND "
              + ConsultaListado.contiene("iq.bien_descripcion", "q")
              + ")",
          "q",
          ConsultaListado.patron(c.q()));
    }
    String orden =
        switch (c.orden()) {
          case ANTIGUAS -> "di.fecha_registro ASC, di.id";
          case ATENCION -> ATENCION + ", di.fecha_registro DESC, di.id";
          case RECIENTES -> "di.fecha_registro DESC, di.id";
          case VENCE -> PRIMER_VENCIMIENTO + " ASC NULLS LAST, di.fecha_registro DESC, di.id";
        };
    List<UUID> ids = consulta.ids(em, "di.id", orden, c.paginado());
    List<DonacionIndependiente> pagina =
        ConsultaListado.enOrden(
            ids,
            ids.isEmpty()
                ? List.of()
                : springDataRepo.findAllById(ids).stream().map(mapper::toDomain).toList(),
            DonacionIndependiente::getId);
    return new Tramo<>(pagina, consulta.total(em));
  }

  @Override
  public Map<TipoEstadoDonacion, Long> contarPorEstado() {
    Map<TipoEstadoDonacion, Long> conteo = new EnumMap<>(TipoEstadoDonacion.class);
    springDataRepo
        .contarPorEstado()
        .forEach(fila -> conteo.put(fila.getEstado(), fila.getCantidad()));
    return conteo;
  }

  @Override
  public long contarQuePasaronA(
      TipoEstadoDonacion estado, LocalDateTime desde, LocalDateTime hasta) {
    Object total =
        em.createNativeQuery(
                "SELECT count(DISTINCT c.id_donacion_independiente)"
                    + " FROM donaciones.cambio_estado_donacion_independiente c"
                    + " JOIN donaciones.estado_donacion_independiente e ON e.id = c.id_estado_nuevo"
                    + " WHERE e.tipo = :estado AND c.\"timestamp\" >= :desde"
                    + " AND c.\"timestamp\" < :hasta")
            .setParameter("estado", estado.name())
            .setParameter("desde", desde)
            .setParameter("hasta", hasta)
            .getSingleResult();
    return ((Number) total).longValue();
  }

  @Override
  public long contarQuePasaronA(
      TipoEstadoDonacion estado,
      LocalDateTime desde,
      LocalDateTime hasta,
      UUID entidadId,
      UUID necesidadId) {
    Query consulta =
        em.createNativeQuery(
                "SELECT count(DISTINCT c.id_donacion_independiente)"
                    + " FROM donaciones.cambio_estado_donacion_independiente c"
                    + " JOIN donaciones.estado_donacion_independiente e ON e.id = c.id_estado_nuevo"
                    + " JOIN donaciones.donacion_independiente di"
                    + " ON di.id = c.id_donacion_independiente"
                    + " WHERE e.tipo = :estado AND c.\"timestamp\" >= :desde"
                    + " AND c.\"timestamp\" < :hasta AND di.id_beneficiario_asignado = :entidad"
                    + (necesidadId == null ? "" : " AND di.id_necesidad = :necesidad"))
            .setParameter("estado", estado.name())
            .setParameter("desde", desde)
            .setParameter("hasta", hasta)
            .setParameter("entidad", entidadId);
    if (necesidadId != null) {
      consulta.setParameter("necesidad", necesidadId);
    }
    return ((Number) consulta.getSingleResult()).longValue();
  }

  @Override
  public Optional<LocalDateTime> primeraFechaRegistro() {
    return Optional.ofNullable(
        em.createQuery(
                "select min(d.fechaRegistro) from DonacionIndependienteEntity d",
                LocalDateTime.class)
            .getSingleResult());
  }

  @Override
  public Map<YearMonth, Long> contarRegistradasPorMes(YearMonth desde, YearMonth hasta) {
    return porMes(
        desde,
        hasta,
        "count(*)",
        "di.fecha_registro",
        " FROM donaciones.donacion_independiente di WHERE",
        null);
  }

  @Override
  public Map<YearMonth, Long> contarQuePasaronAPorMes(
      TipoEstadoDonacion estado, YearMonth desde, YearMonth hasta) {
    return porMes(
        desde,
        hasta,
        "count(DISTINCT c.id_donacion_independiente)",
        "c.\"timestamp\"",
        PASARON_A + " WHERE e.tipo = :estado AND",
        estado);
  }

  /**
   * Una sola consulta con una columna por mes ({@code FILTER}): los límites de cada mes van como
   * parámetros, así se comparan con la misma conversión de zona con que se guardaron las fechas.
   */
  private Map<YearMonth, Long> porMes(
      YearMonth desde,
      YearMonth hasta,
      String contar,
      String fecha,
      String desdeDonde,
      TipoEstadoDonacion estado) {
    List<YearMonth> meses = new ArrayList<>();
    for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
      meses.add(mes);
    }
    if (meses.isEmpty()) {
      return Map.of();
    }
    StringBuilder sql = new StringBuilder("SELECT ");
    for (int i = 0; i < meses.size(); i++) {
      sql.append(i == 0 ? "" : ", ")
          .append(contar)
          .append(" FILTER (WHERE ")
          .append(fecha)
          .append(" >= :b")
          .append(i)
          .append(" AND ")
          .append(fecha)
          .append(" < :b")
          .append(i + 1)
          .append(")");
    }
    sql.append(desdeDonde)
        .append(' ')
        .append(fecha)
        .append(" >= :b0 AND ")
        .append(fecha)
        .append(" < :b")
        .append(meses.size());
    Query consulta = em.createNativeQuery(sql.toString());
    for (int i = 0; i <= meses.size(); i++) {
      consulta.setParameter("b" + i, desde.plusMonths(i).atDay(1).atStartOfDay());
    }
    if (estado != null) {
      consulta.setParameter("estado", estado.name());
    }
    Object resultado = consulta.getSingleResult();
    Object[] fila = resultado instanceof Object[] columnas ? columnas : new Object[] {resultado};
    Map<YearMonth, Long> conteo = new HashMap<>();
    for (int i = 0; i < meses.size(); i++) {
      conteo.put(meses.get(i), ((Number) fila[i]).longValue());
    }
    return conteo;
  }

  @Override
  public Map<UUID, Long> contarRegistradasPorCategoria(LocalDateTime desde, LocalDateTime hasta) {
    return porCategoria(
        em.createNativeQuery(
                "SELECT s.id_categoria, count(DISTINCT di.id)"
                    + " FROM donaciones.donacion_independiente di"
                    + " JOIN donaciones.item_donacion_independiente i"
                    + " ON i.id_donacion_independiente = di.id"
                    + CATEGORIA_DE_LOS_ITEMS
                    + " WHERE di.fecha_registro >= :desde AND di.fecha_registro < :hasta"
                    + " GROUP BY s.id_categoria")
            .setParameter("desde", desde)
            .setParameter("hasta", hasta));
  }

  @Override
  public Map<UUID, Long> contarQuePasaronAPorCategoria(
      TipoEstadoDonacion estado, LocalDateTime desde, LocalDateTime hasta) {
    return porCategoria(
        em.createNativeQuery(
                "SELECT s.id_categoria, count(DISTINCT c.id_donacion_independiente)"
                    + PASARON_A
                    + " JOIN donaciones.item_donacion_independiente i"
                    + " ON i.id_donacion_independiente = c.id_donacion_independiente"
                    + CATEGORIA_DE_LOS_ITEMS
                    + " WHERE"
                    + EN_EL_TRAMO_DEL_CAMBIO
                    + " GROUP BY s.id_categoria")
            .setParameter("estado", estado.name())
            .setParameter("desde", desde)
            .setParameter("hasta", hasta));
  }

  private static Map<UUID, Long> porCategoria(Query consulta) {
    Map<UUID, Long> conteo = new HashMap<>();
    List<?> filas = consulta.getResultList();
    filas.stream()
        .map(Object[].class::cast)
        .forEach(fila -> conteo.put(ConsultaListado.uuid(fila[0]), ((Number) fila[1]).longValue()));
    return conteo;
  }

  @Override
  public List<ConteoAsignadas> contarAsignadas(UUID entidadId, UUID necesidadId) {
    Query consulta =
        em.createNativeQuery(
            "SELECT di.id_necesidad, di.estado_actual, count(*)"
                + " FROM donaciones.donacion_independiente di"
                + " WHERE di.id_beneficiario_asignado = :entidad"
                + (necesidadId == null ? "" : " AND di.id_necesidad = :necesidad")
                + " GROUP BY di.id_necesidad, di.estado_actual");
    consulta.setParameter("entidad", entidadId);
    if (necesidadId != null) {
      consulta.setParameter("necesidad", necesidadId);
    }
    List<?> filas = consulta.getResultList();
    return filas.stream()
        .map(Object[].class::cast)
        .map(
            fila ->
                new ConteoAsignadas(
                    fila[0] == null ? null : ConsultaListado.uuid(fila[0]),
                    TipoEstadoDonacion.valueOf((String) fila[1]),
                    ((Number) fila[2]).longValue()))
        .toList();
  }

  @Override
  public PorVencer buscarPorVencer(LocalDate desde, LocalDate hasta) {
    Object cantidad =
        em.createNativeQuery("SELECT count(DISTINCT di.id)" + POR_VENCER)
            .setParameter("desde", desde)
            .setParameter("hasta", hasta)
            .getSingleResult();
    List<?> proxima =
        em.createNativeQuery(
                "SELECT di.id, min(n.fecha_vencimiento) AS vence"
                    + POR_VENCER
                    + " GROUP BY di.id ORDER BY vence, di.id LIMIT 1")
            .setParameter("desde", desde)
            .setParameter("hasta", hasta)
            .getResultList();
    if (proxima.isEmpty()) {
      return PorVencer.ninguna();
    }
    Object[] fila = (Object[]) proxima.get(0);
    return new PorVencer(
        ((Number) cantidad).longValue(), ConsultaListado.uuid(fila[0]), fecha(fila[1]));
  }

  private static LocalDate fecha(Object valor) {
    return valor instanceof Date sql ? sql.toLocalDate() : (LocalDate) valor;
  }

  // Las fotos son una colección lazy: se mapean a dominio con la sesión abierta
  @Override
  @Transactional(readOnly = true)
  public List<DonacionIndependiente> findDestacadasDesde(LocalDateTime desde, int limite) {
    return springDataRepo
        .findByEstadoActualAndFechaEntregaGreaterThanEqualOrderByFechaEntregaDescIdAsc(
            TipoEstadoDonacion.ENTREGADA, desde, Limit.of(limite))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long contarDestacadasDesde(LocalDateTime desde) {
    return springDataRepo.countByEstadoActualAndFechaEntregaGreaterThanEqual(
        TipoEstadoDonacion.ENTREGADA, desde);
  }
}
