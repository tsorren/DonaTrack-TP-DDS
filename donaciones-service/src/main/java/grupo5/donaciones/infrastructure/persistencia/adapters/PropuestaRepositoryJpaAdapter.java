package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.propuestas.PropuestaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.PropuestasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataPropuestaRepository;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.listados.BusquedaDecididas;
import grupo5.donaciones.models.repositories.listados.CriterioPropuestas;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.PropuestaEnListado;
import grupo5.donaciones.models.repositories.listados.StockEnPropuestas;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class PropuestaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Propuesta, PropuestaEntity, SpringDataPropuestaRepository>
    implements IPropuestasRepository {

  // R11-24: lo que piden de cada donación las pendientes
  private static final String PEDIDO =
      "SELECT f.id_donacion_independiente AS di, sum(f.cantidad) AS total,"
          + " count(DISTINCT f.id_propuesta) AS propuestas"
          + " FROM donaciones.posible_fragmentacion f"
          + " JOIN donaciones.propuesta pp ON pp.id = f.id_propuesta AND pp.estado = 'PENDIENTE'"
          + " GROUP BY f.id_donacion_independiente";

  // Lo que queda de la donación d en el depósito: 0 si ya no está
  private static final String DISPONIBLE =
      "CASE WHEN d.estado_actual = 'EN_DEPOSITO' THEN coalesce((SELECT sum(i.cantidad)"
          + " FROM donaciones.item_donacion_independiente i"
          + " WHERE i.id_donacion_independiente = d.id), 0) ELSE 0 END";

  // Cada donación pedida por alguna pendiente, con lo pedido y lo que queda
  private static final String STOCK =
      "SELECT pe.di, pe.total, pe.propuestas, d.fecha_registro, "
          + DISPONIBLE
          + " AS disponible FROM ("
          + PEDIDO
          + ") pe JOIN donaciones.donacion_independiente d ON d.id = pe.di";

  /*
   * La situación de cada pendiente (spec R11-24, §3.1): SIN_STOCK si alguna fragmentación pide más
   * de lo que queda; EN_CONFLICTO si alguna de sus donaciones no alcanza para todas las pendientes
   * que la piden (pedir más que "disponible - lo que piden las otras" es lo mismo que "lo que piden
   * todas > disponible") o si otra pendiente cubre la misma necesidad; si no, LIBRE. clave: la
   * primera donación que no alcanza, para que las que compiten salgan juntas.
   */
  private static final String SITUACION =
      "SELECT q.id, q.otras, q.clave, CASE WHEN q.sin_stock THEN 'SIN_STOCK'"
          + " WHEN q.comparte OR q.otras > 0 THEN 'EN_CONFLICTO' ELSE 'LIBRE' END AS situacion"
          + " FROM (SELECT pend.id,"
          + " (SELECT count(*) FROM donaciones.propuesta o WHERE o.estado = 'PENDIENTE'"
          + " AND o.id_necesidad_que_satisface = pend.id_necesidad_que_satisface"
          + " AND o.id <> pend.id) AS otras,"
          + " coalesce(bool_or(f.cantidad > s.disponible), false) AS sin_stock,"
          + " coalesce(bool_or(s.total > s.disponible), false) AS comparte,"
          + " min(CASE WHEN s.total > s.disponible THEN f.id_donacion_independiente::text END)"
          + " AS clave"
          + " FROM donaciones.propuesta pend"
          + " LEFT JOIN donaciones.posible_fragmentacion f ON f.id_propuesta = pend.id"
          + " LEFT JOIN ("
          + STOCK
          + ") s ON s.di = f.id_donacion_independiente"
          + " WHERE pend.estado = 'PENDIENTE'"
          + " GROUP BY pend.id, pend.id_necesidad_que_satisface) q";

  private static final String DE_LA_CATEGORIA =
      "EXISTS (SELECT 1 FROM donaciones.item_donacion_independiente ic"
          + " JOIN donaciones.item_donacion_normalizado nc ON nc.id = ic.id_item_normalizado"
          + " JOIN donaciones.subcategoria sc ON sc.id = nc.id_subcategoria"
          + " WHERE ic.id_donacion_independiente = s.di AND sc.id_categoria = :categoriaId)";

  private static final String ALGORITMO = "algoritmo";

  // R11-35: el texto buscado, en la organización y lo que pide la necesidad, o en lo donado
  private static final String BUSCADA =
      "(EXISTS (SELECT 1 FROM donaciones.necesidad nq"
          + " LEFT JOIN donaciones.juridica jq ON jq.id = nq.entidad_beneficiaria_id"
          + " WHERE nq.id = p.id_necesidad_que_satisface AND ("
          + ConsultaListado.contiene("jq.razon_social", "q")
          + " OR "
          + ConsultaListado.contiene("nq.descripcion", "q")
          + ")) OR EXISTS (SELECT 1 FROM donaciones.posible_fragmentacion fq"
          + " JOIN donaciones.item_donacion_independiente iq"
          + " ON iq.id_donacion_independiente = fq.id_donacion_independiente"
          + " WHERE fq.id_propuesta = p.id AND "
          + ConsultaListado.contiene("iq.bien_descripcion", "q")
          + "))";

  // R11-35: la categoría de la necesidad que cubre
  private static final String DE_LA_CATEGORIA_DE_LA_NECESIDAD =
      "EXISTS (SELECT 1 FROM donaciones.necesidad nc"
          + " JOIN donaciones.subcategoria snc ON snc.id = nc.subcategoria_id"
          + " WHERE nc.id = p.id_necesidad_que_satisface"
          + " AND snc.id_categoria = :categoriaDecidida)";

  private static final String DECIDIDAS =
      "p.estado IN ('" + EstadoPropuesta.APROBADA + "', '" + EstadoPropuesta.DESCARTADA + "')";

  private final PropuestasPersistenciaMapper mapper;

  @PersistenceContext private EntityManager em;

  public PropuestaRepositoryJpaAdapter(
      SpringDataPropuestaRepository springDataRepo, PropuestasPersistenciaMapper mapper) {
    super(springDataRepo, p -> mapper.toEntity(p, null), mapper::toDomain);
    this.mapper = mapper;
  }

  /** Parte de la fila guardada, para reemplazar las fragmentaciones en la misma transacción. */
  @Override
  @Transactional
  public Propuesta save(Propuesta propuesta) {
    PropuestaEntity existente = springDataRepo.findById(propuesta.getId()).orElse(null);
    return mapper.toDomain(springDataRepo.save(mapper.toEntity(propuesta, existente)));
  }

  @Override
  @Transactional
  public List<Propuesta> saveAll(List<Propuesta> propuestas) {
    return propuestas.stream().map(this::save).toList();
  }

  // Las fragmentaciones son una colección lazy: se mapean a dominio con la sesión abierta
  @Override
  @Transactional(readOnly = true)
  public Optional<Propuesta> findById(UUID id) {
    return super.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Propuesta> findAll() {
    return super.findAll();
  }

  @Override
  public long contarEnEstado(EstadoPropuesta estado) {
    return springDataRepo.countByEstado(estado);
  }

  @Override
  @Transactional(readOnly = true)
  public Tramo<PropuestaEnListado> buscar(CriterioPropuestas c) {
    String algoritmo = c.algoritmo() == null ? null : c.algoritmo().name();
    ConsultaListado consulta =
        new ConsultaListado(
                "FROM donaciones.propuesta p LEFT JOIN (" + SITUACION + ") sit ON sit.id = p.id")
            .siHay(
                c.estados().stream().map(EstadoPropuesta::name).toList(),
                "p.estado IN (:estados)",
                "estados")
            .siHay(
                c.situaciones().stream().map(SituacionPropuesta::name).toList(),
                "sit.situacion IN (:situaciones)",
                "situaciones")
            .siHay(
                algoritmo,
                "EXISTS (SELECT 1 FROM donaciones.sugerencia_propuesta sa"
                    + " WHERE sa.id_propuesta = p.id AND sa.algoritmo = :algoritmo)",
                ALGORITMO)
            .siHay(
                c.donacionId(),
                "EXISTS (SELECT 1 FROM donaciones.posible_fragmentacion fd"
                    + " WHERE fd.id_propuesta = p.id"
                    + " AND fd.id_donacion_independiente = :donacionId)",
                "donacionId");
    buscarDecididas(consulta, c.decididas());
    if (c.ambos()) {
      consulta.donde(
          "(SELECT count(DISTINCT sb.algoritmo) FROM donaciones.sugerencia_propuesta sb"
              + " WHERE sb.id_propuesta = p.id) > 1");
    }
    List<UUID> ids = consulta.ids(em, "p.id", orden(c, algoritmo), c.paginado());
    long total = consulta.total(em);
    if (ids.isEmpty()) {
      return new Tramo<>(List.of(), total);
    }
    List<Propuesta> pagina =
        ConsultaListado.enOrden(
            ids,
            springDataRepo.findAllById(ids).stream().map(mapper::toDomain).toList(),
            Propuesta::getId);
    Map<UUID, Object[]> situaciones = situacionesDe(ids);
    Map<UUID, StockEnPropuestas> stocks =
        stocksDe(
            pagina.stream()
                .flatMap(p -> p.getPosiblesFragmentaciones().stream())
                .map(PosibleFragmentacion::getDonacionOriginalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    return new Tramo<>(
        pagina.stream().map(p -> enListado(p, situaciones.get(p.getId()), stocks)).toList(), total);
  }

  /** La búsqueda de Todas las decididas (R11-35): solo las condiciones de lo que se pidió. */
  private static void buscarDecididas(ConsultaListado consulta, BusquedaDecididas b) {
    if (b.q() != null) {
      consulta.donde(BUSCADA, "q", ConsultaListado.patron(b.q()));
    }
    consulta.siHay(b.categoriaId(), DE_LA_CATEGORIA_DE_LA_NECESIDAD, "categoriaDecidida");
    if (b.deSistema()) {
      consulta.donde("(p.decidida_por IS NULL OR p.decidida_por = :decididaPor)");
      consulta.parametro("decididaPor", BusquedaDecididas.SISTEMA);
    } else {
      consulta.siHay(b.decididaPor(), "p.decidida_por = :decididaPor", "decididaPor");
    }
    consulta
        .siHay(
            b.desde() == null ? null : b.desde().atStartOfDay(),
            "p.fecha_decision >= :decididaDesde",
            "decididaDesde")
        .siHay(
            b.hasta() == null ? null : b.hasta().plusDays(1).atStartOfDay(),
            "p.fecha_decision < :decididaHasta",
            "decididaHasta");
  }

  /** El orden del listado; desempata siempre por id. */
  private static String orden(CriterioPropuestas c, String algoritmo) {
    String recientes = "p.fecha_creacion DESC, p.id";
    String orden =
        switch (c.orden()) {
          case RECIENTE -> recientes;
          case DECISION_RECIENTE -> "p.fecha_decision DESC NULLS LAST, " + recientes;
          case DECISION_ANTIGUA -> "p.fecha_decision ASC NULLS FIRST, p.fecha_creacion ASC, p.id";
          case PUESTO ->
              "(SELECT min(sp.puesto) FROM donaciones.sugerencia_propuesta sp"
                  + " WHERE sp.id_propuesta = p.id"
                  + (algoritmo == null ? "" : " AND sp.algoritmo = :algoritmo")
                  + ") ASC NULLS LAST, "
                  + recientes;
        };
    return c.agrupaPorDonacion() ? "sit.clave ASC NULLS LAST, " + orden : orden;
  }

  private static PropuestaEnListado enListado(
      Propuesta p, Object[] situacion, Map<UUID, StockEnPropuestas> stocks) {
    Map<UUID, StockEnPropuestas> porDonacion = new HashMap<>();
    p.getPosiblesFragmentaciones().stream()
        .map(PosibleFragmentacion::getDonacionOriginalId)
        .filter(stocks::containsKey)
        .forEach(id -> porDonacion.put(id, stocks.get(id)));
    return new PropuestaEnListado(
        p,
        situacion == null ? null : SituacionPropuesta.valueOf(situacion[2].toString()),
        situacion == null ? 0 : entero(situacion[1]),
        porDonacion);
  }

  /** Por id: el id, cuántas otras pendientes cubren la misma necesidad y la situación. */
  private Map<UUID, Object[]> situacionesDe(List<UUID> ids) {
    List<?> filas =
        em.createNativeQuery(
                "SELECT s.id, s.otras, s.situacion FROM (" + SITUACION + ") s WHERE s.id IN (:ids)")
            .setParameter("ids", ids)
            .getResultList();
    Map<UUID, Object[]> porId = new HashMap<>();
    for (Object fila : filas) {
      Object[] columnas = (Object[]) fila;
      porId.put(ConsultaListado.uuid(columnas[0]), columnas);
    }
    return porId;
  }

  /** Lo que queda en el depósito de cada donación y lo que piden de ella las pendientes. */
  private Map<UUID, StockEnPropuestas> stocksDe(Collection<UUID> donaciones) {
    if (donaciones.isEmpty()) {
      return Map.of();
    }
    List<?> filas =
        em.createNativeQuery(
                "SELECT d.id, "
                    + DISPONIBLE
                    + ", coalesce(pe.total, 0), coalesce(pe.propuestas, 0)"
                    + " FROM donaciones.donacion_independiente d LEFT JOIN ("
                    + PEDIDO
                    + ") pe ON pe.di = d.id WHERE d.id IN (:ids)")
            .setParameter("ids", donaciones)
            .getResultList();
    Map<UUID, StockEnPropuestas> porId = new HashMap<>();
    for (Object fila : filas) {
      Object[] c = (Object[]) fila;
      UUID id = ConsultaListado.uuid(c[0]);
      porId.put(id, new StockEnPropuestas(id, entero(c[1]), entero(c[2]), entero(c[3])));
    }
    return porId;
  }

  private static int entero(Object valor) {
    return valor == null ? 0 : ((Number) valor).intValue();
  }

  @Override
  public long contarDecididasSinFecha() {
    return ((Number)
            em.createNativeQuery(
                    "SELECT count(*) FROM donaciones.propuesta p WHERE "
                        + DECIDIDAS
                        + " AND p.fecha_decision IS NULL")
                .getSingleResult())
        .longValue();
  }

  @Override
  public Map<SituacionPropuesta, Long> contarPendientesPorSituacion() {
    Map<SituacionPropuesta, Long> conteo = new EnumMap<>(SituacionPropuesta.class);
    for (SituacionPropuesta s : SituacionPropuesta.values()) {
      conteo.put(s, 0L);
    }
    List<?> filas =
        em.createNativeQuery(
                "SELECT s.situacion, count(*) FROM (" + SITUACION + ") s GROUP BY s.situacion")
            .getResultList();
    for (Object fila : filas) {
      Object[] c = (Object[]) fila;
      conteo.put(SituacionPropuesta.valueOf(c[0].toString()), ((Number) c[1]).longValue());
    }
    return conteo;
  }

  @Override
  @Transactional(readOnly = true)
  public Tramo<StockEnPropuestas> buscarDonacionesEnPendientes(
      UUID categoriaId, boolean soloEnConflicto, Paginado paginado) {
    ConsultaListado consulta =
        new ConsultaListado("FROM (" + STOCK + ") s")
            .siHay(categoriaId, DE_LA_CATEGORIA, "categoriaId");
    if (soloEnConflicto) {
      consulta.donde("s.total > s.disponible");
    }
    List<UUID> ids =
        consulta.ids(
            em, "s.di", "(s.total > s.disponible) DESC, s.fecha_registro ASC, s.di", paginado);
    long total = consulta.total(em);
    Map<UUID, StockEnPropuestas> stocks = stocksDe(ids);
    return new Tramo<>(ids.stream().map(stocks::get).filter(Objects::nonNull).toList(), total);
  }
}
