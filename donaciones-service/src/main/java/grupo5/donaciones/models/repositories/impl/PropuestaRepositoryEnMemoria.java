package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import grupo5.donaciones.models.entities.propuestas.SugerenciaDeAlgoritmo;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.listados.BusquedaDecididas;
import grupo5.donaciones.models.repositories.listados.CriterioPropuestas;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.PropuestaEnListado;
import grupo5.donaciones.models.repositories.listados.StockEnPropuestas;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Las propuestas sin base (perfil distinto de {@code postgres}). La situación de cada pendiente y
 * los filtros del listado (R11-24) se calculan sobre lo guardado en memoria, con la misma regla que
 * la consulta de PostgreSQL ({@link SituacionPropuesta#de}). La búsqueda de las decididas (R11-35)
 * mira lo que pide la necesidad y lo donado; el nombre de la organización solo lo busca PostgreSQL.
 */
@Repository
@Profile("!postgres")
public class PropuestaRepositoryEnMemoria extends CrudRepositoryEnMemoria<Propuesta>
    implements IPropuestasRepository {

  private final IDonacionesIndependientesRepository donaciones;
  private final ISubcategoriasRepository subcategorias;
  private final INecesidadesRepository necesidades;

  /**
   * Sin depósito: toda donación pedida cuenta como que ya no está (para los tests que no lo usan).
   */
  public PropuestaRepositoryEnMemoria() {
    this(null, null, null);
  }

  @Autowired
  public PropuestaRepositoryEnMemoria(
      IDonacionesIndependientesRepository donaciones,
      ISubcategoriasRepository subcategorias,
      INecesidadesRepository necesidades) {
    this.donaciones = donaciones;
    this.subcategorias = subcategorias;
    this.necesidades = necesidades;
  }

  @Override
  public long contarDecididasSinFecha() {
    return storage.values().stream()
        .filter(p -> p.getEstado() != EstadoPropuesta.PENDIENTE)
        .filter(p -> p.getFechaDecision() == null)
        .count();
  }

  @Override
  public long contarEnEstado(EstadoPropuesta estado) {
    return storage.values().stream().filter(p -> p.getEstado() == estado).count();
  }

  @Override
  public Tramo<PropuestaEnListado> buscar(CriterioPropuestas c) {
    Foto foto = new Foto();
    Comparator<PropuestaEnListado> orden =
        switch (c.orden()) {
          case RECIENTE -> recientes();
          case DECISION_RECIENTE ->
              Comparator.comparing(
                      (PropuestaEnListado p) -> p.propuesta().getFechaDecision(),
                      Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))
                  .thenComparing(recientes());
          case DECISION_ANTIGUA ->
              Comparator.comparing(
                      (PropuestaEnListado p) -> p.propuesta().getFechaDecision(),
                      Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
                  .thenComparing(
                      p -> p.propuesta().getFechaCreacion(),
                      Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()))
                  .thenComparing(p -> p.propuesta().getId());
          case PUESTO ->
              Comparator.comparing(
                      (PropuestaEnListado p) -> mejorPuesto(p.propuesta(), c),
                      Comparator.nullsLast(Comparator.naturalOrder()))
                  .thenComparing(recientes());
        };
    if (c.agrupaPorDonacion()) {
      orden =
          Comparator.comparing(
                  (PropuestaEnListado p) -> foto.donacionEnConflicto(p.propuesta()),
                  Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(orden);
    }
    List<PropuestaEnListado> filtradas =
        storage.values().stream()
            .filter(p -> c.estados().isEmpty() || c.estados().contains(p.getEstado()))
            .filter(p -> c.donacionId() == null || pide(p, c.donacionId()))
            .filter(p -> sugeridaSegun(p, c))
            .filter(p -> coincide(p, c.decididas()))
            .map(foto::enListado)
            .filter(p -> c.situaciones().isEmpty() || c.situaciones().contains(p.situacion()))
            .sorted(orden)
            .toList();
    return Tramo.de(filtradas, c.paginado());
  }

  @Override
  public Map<SituacionPropuesta, Long> contarPendientesPorSituacion() {
    Foto foto = new Foto();
    Map<SituacionPropuesta, Long> conteo = new EnumMap<>(SituacionPropuesta.class);
    for (SituacionPropuesta s : SituacionPropuesta.values()) {
      conteo.put(s, 0L);
    }
    foto.pendientes.forEach(p -> conteo.merge(foto.enListado(p).situacion(), 1L, Long::sum));
    return conteo;
  }

  @Override
  public Tramo<StockEnPropuestas> buscarDonacionesEnPendientes(
      UUID categoriaId, boolean soloEnConflicto, Paginado paginado) {
    Foto foto = new Foto();
    Set<UUID> deLaCategoria = categoriaId == null ? Set.of() : subcategoriasDe(categoriaId);
    List<StockEnPropuestas> filtradas =
        foto.pedidoPorDonacion.keySet().stream()
            .map(foto::stock)
            .filter(s -> !soloEnConflicto || !s.alcanza())
            .filter(s -> categoriaId == null || deLaCategoria(s.donacionId(), deLaCategoria))
            .sorted(
                Comparator.comparing(StockEnPropuestas::alcanza)
                    .thenComparing(
                        s -> foto.registro(s.donacionId()),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StockEnPropuestas::donacionId))
            .toList();
    return Tramo.de(filtradas, paginado);
  }

  private static Comparator<PropuestaEnListado> recientes() {
    return Comparator.comparing(
            (PropuestaEnListado p) -> p.propuesta().getFechaCreacion(),
            Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))
        .thenComparing(p -> p.propuesta().getId());
  }

  private static Integer mejorPuesto(Propuesta p, CriterioPropuestas c) {
    return p.getSugerencias().stream()
        .filter(s -> c.algoritmo() == null || s.algoritmo() == c.algoritmo())
        .map(SugerenciaDeAlgoritmo::puesto)
        .min(Integer::compare)
        .orElse(null);
  }

  private static boolean sugeridaSegun(Propuesta p, CriterioPropuestas c) {
    if (c.ambos()) {
      return p.getSugerencias().stream().map(SugerenciaDeAlgoritmo::algoritmo).distinct().count()
          > 1;
    }
    return c.algoritmo() == null
        || p.getSugerencias().stream().anyMatch(s -> s.algoritmo() == c.algoritmo());
  }

  /** La búsqueda de las decididas (R11-35), con la misma regla que la consulta de PostgreSQL. */
  private boolean coincide(Propuesta p, BusquedaDecididas b) {
    if (b.deSistema()) {
      if (p.getDecididaPor() != null && !BusquedaDecididas.SISTEMA.equals(p.getDecididaPor())) {
        return false;
      }
    } else if (b.decididaPor() != null && !b.decididaPor().equals(p.getDecididaPor())) {
      return false;
    }
    if (!enElPeriodo(p.getFechaDecision(), b)) {
      return false;
    }
    Optional<Necesidad> necesidad =
        necesidades == null || p.getNecesidadQueSatisfaceId() == null
            ? Optional.empty()
            : necesidades.findById(p.getNecesidadQueSatisfaceId());
    if (b.categoriaId() != null
        && necesidad
            .map(n -> !subcategoriasDe(b.categoriaId()).contains(n.getSubcategoriaId()))
            .orElse(true)) {
      return false;
    }
    return b.q() == null
        || necesidad.map(n -> TextoBuscado.contiene(n.getDescripcion(), b.q())).orElse(false)
        || p.getPosiblesFragmentaciones().stream()
            .map(PosibleFragmentacion::getDonacionOriginalId)
            .map(this::donacion)
            .flatMap(Optional::stream)
            .flatMap(d -> d.getItems().stream())
            .anyMatch(i -> TextoBuscado.contiene(i.bien().bienOriginal().descripcion(), b.q()));
  }

  private static boolean enElPeriodo(LocalDateTime fecha, BusquedaDecididas b) {
    if (b.desde() == null && b.hasta() == null) {
      return true;
    }
    if (fecha == null) {
      return false;
    }
    return (b.desde() == null || !fecha.toLocalDate().isBefore(b.desde()))
        && (b.hasta() == null || !fecha.toLocalDate().isAfter(b.hasta()));
  }

  private static boolean pide(Propuesta p, UUID donacion) {
    return p.getPosiblesFragmentaciones().stream()
        .anyMatch(f -> donacion.equals(f.getDonacionOriginalId()));
  }

  private Set<UUID> subcategoriasDe(UUID categoriaId) {
    if (subcategorias == null) {
      return Set.of();
    }
    return subcategorias.findAll().stream()
        .filter(s -> categoriaId.equals(s.getCategoriaId()))
        .map(Subcategoria::getId)
        .collect(Collectors.toSet());
  }

  private boolean deLaCategoria(UUID donacionId, Set<UUID> deLaCategoria) {
    return donacion(donacionId)
        .map(
            d ->
                d.getItems().stream()
                    .anyMatch(i -> deLaCategoria.contains(i.bien().subcategoriaId())))
        .orElse(false);
  }

  private Optional<DonacionIndependiente> donacion(UUID id) {
    return donaciones == null || id == null ? Optional.empty() : donaciones.findById(id);
  }

  /** Las pendientes de este momento y lo que piden de cada donación, leídas una vez. */
  private final class Foto {

    private final List<Propuesta> pendientes =
        storage.values().stream().filter(p -> p.getEstado() == EstadoPropuesta.PENDIENTE).toList();
    private final Map<UUID, Integer> pedidoPorDonacion = new HashMap<>();
    private final Map<UUID, Integer> propuestasPorDonacion = new HashMap<>();
    private final Map<UUID, Integer> pendientesPorNecesidad = new HashMap<>();
    private final Map<UUID, StockEnPropuestas> stocks = new HashMap<>();

    Foto() {
      for (Propuesta p : pendientes) {
        pendientesPorNecesidad.merge(p.getNecesidadQueSatisfaceId(), 1, Integer::sum);
        p.getPosiblesFragmentaciones().stream()
            .map(PosibleFragmentacion::getDonacionOriginalId)
            .filter(Objects::nonNull)
            .distinct()
            .forEach(id -> propuestasPorDonacion.merge(id, 1, Integer::sum));
        for (PosibleFragmentacion f : p.getPosiblesFragmentaciones()) {
          if (f.getDonacionOriginalId() != null) {
            pedidoPorDonacion.merge(f.getDonacionOriginalId(), cantidad(f), Integer::sum);
          }
        }
      }
    }

    StockEnPropuestas stock(UUID donacionId) {
      return stocks.computeIfAbsent(
          donacionId,
          id ->
              new StockEnPropuestas(
                  id,
                  donacion(id)
                      .filter(
                          d ->
                              d.getEstadoActual() != null
                                  && d.getEstadoActual().getTipo()
                                      == TipoEstadoDonacion.EN_DEPOSITO)
                      .map(DonacionIndependiente::getCantidad)
                      .orElse(0),
                  pedidoPorDonacion.getOrDefault(id, 0),
                  propuestasPorDonacion.getOrDefault(id, 0)));
    }

    LocalDateTime registro(UUID donacionId) {
      return donacion(donacionId).map(DonacionIndependiente::getFechaRegistro).orElse(null);
    }

    PropuestaEnListado enListado(Propuesta p) {
      Map<UUID, StockEnPropuestas> porDonacion = new HashMap<>();
      p.getPosiblesFragmentaciones().stream()
          .map(PosibleFragmentacion::getDonacionOriginalId)
          .filter(Objects::nonNull)
          .forEach(id -> porDonacion.put(id, stock(id)));
      if (p.getEstado() != EstadoPropuesta.PENDIENTE) {
        return new PropuestaEnListado(p, null, otrasDe(p, 0), porDonacion);
      }
      int otras = otrasDe(p, 1);
      SituacionPropuesta situacion =
          SituacionPropuesta.de(
              p.getPosiblesFragmentaciones().stream()
                  .filter(f -> f.getDonacionOriginalId() != null)
                  .map(
                      f -> {
                        StockEnPropuestas s = porDonacion.get(f.getDonacionOriginalId());
                        return new SituacionPropuesta.Pedido(
                            cantidad(f), s.disponible(), s.pedidaEnPendientes() - cantidad(f));
                      })
                  .toList(),
              otras);
      return new PropuestaEnListado(p, situacion, otras, porDonacion);
    }

    /** La primera donación (por id) que no alcanza para las pendientes que la piden. */
    UUID donacionEnConflicto(Propuesta p) {
      return p.getPosiblesFragmentaciones().stream()
          .map(PosibleFragmentacion::getDonacionOriginalId)
          .filter(Objects::nonNull)
          .filter(id -> !stock(id).alcanza())
          .min(Comparator.naturalOrder())
          .orElse(null);
    }

    private int otrasDe(Propuesta p, int propia) {
      return pendientesPorNecesidad.getOrDefault(p.getNecesidadQueSatisfaceId(), 0) - propia;
    }
  }

  private static int cantidad(PosibleFragmentacion f) {
    return f.getCantidadNecesaria() == null ? 0 : f.getCantidadNecesaria();
  }
}
