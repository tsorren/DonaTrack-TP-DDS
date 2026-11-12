package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.CambioEstado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.listados.ConteoAsignadas;
import grupo5.donaciones.models.repositories.listados.CriterioDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.OrdenDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.PorVencer;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class DonacionesIndependientesRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<DonacionIndependiente>
    implements IDonacionesIndependientesRepository {

  private final IDonacionesRepository donaciones;
  private final ISubcategoriasRepository subcategorias;

  /** Sin búsqueda por donante ni por categoría: para los tests que no la usan. */
  public DonacionesIndependientesRepositoryEnMemoria() {
    this(null, null);
  }

  @Autowired
  public DonacionesIndependientesRepositoryEnMemoria(
      IDonacionesRepository donaciones, ISubcategoriasRepository subcategorias) {
    this.donaciones = donaciones;
    this.subcategorias = subcategorias;
  }

  @Override
  public Tramo<DonacionIndependiente> buscar(CriterioDonacionesIndependientes c) {
    Set<UUID> delDonante = c.donanteId() == null ? Set.of() : donacionesDe(c.donanteId());
    Set<UUID> deLaCategoria = c.categoriaId() == null ? Set.of() : subcategoriasDe(c.categoriaId());
    Comparator<DonacionIndependiente> porFecha =
        Comparator.comparing(DonacionIndependiente::getFechaRegistro);
    if (c.orden() != OrdenDonacionesIndependientes.ANTIGUAS) {
      porFecha = porFecha.reversed();
    }
    if (c.orden() == OrdenDonacionesIndependientes.ATENCION) {
      porFecha =
          Comparator.comparingInt(
                  (DonacionIndependiente di) ->
                      OrdenDonacionesIndependientes.grupoDeAtencion(tipo(di)))
              .thenComparing(porFecha);
    }
    List<DonacionIndependiente> filtradas =
        storage.values().stream()
            .filter(di -> c.estados().isEmpty() || c.estados().contains(tipo(di)))
            .filter(di -> c.donanteId() == null || delDonante.contains(di.getDonacionOriginalId()))
            .filter(
                di ->
                    c.subcategoriaId() == null || tieneSubcategoria(di, Set.of(c.subcategoriaId())))
            .filter(di -> c.categoriaId() == null || tieneSubcategoria(di, deLaCategoria))
            .filter(
                di -> c.entidadId() == null || c.entidadId().equals(di.getEntidadBeneficiariaId()))
            .filter(di -> c.necesidadId() == null || c.necesidadId().equals(di.getNecesidadId()))
            .filter(di -> c.id() == null || c.id().equals(di.getId()))
            .filter(
                di -> c.desde() == null || !di.getFechaRegistro().toLocalDate().isBefore(c.desde()))
            .filter(
                di -> c.hasta() == null || !di.getFechaRegistro().toLocalDate().isAfter(c.hasta()))
            .filter(
                di ->
                    c.q() == null
                        || di.getItems().stream()
                            .anyMatch(
                                i ->
                                    TextoBuscado.contiene(
                                        i.bien().bienOriginal().descripcion(), c.q())))
            .sorted(porFecha.thenComparing(DonacionIndependiente::getId))
            .toList();
    return Tramo.de(filtradas, c.paginado());
  }

  private static TipoEstadoDonacion tipo(DonacionIndependiente di) {
    return di.getEstadoActual() == null ? null : di.getEstadoActual().getTipo();
  }

  private Set<UUID> donacionesDe(UUID donanteId) {
    return donaciones.findAll().stream()
        .filter(d -> donanteId.equals(d.getDonanteId()))
        .map(Donacion::getId)
        .collect(Collectors.toSet());
  }

  private Set<UUID> subcategoriasDe(UUID categoriaId) {
    return subcategorias.findAll().stream()
        .filter(s -> categoriaId.equals(s.getCategoriaId()))
        .map(Subcategoria::getId)
        .collect(Collectors.toSet());
  }

  private static boolean tieneSubcategoria(DonacionIndependiente di, Set<UUID> buscadas) {
    return di.getItems().stream().anyMatch(i -> buscadas.contains(i.bien().subcategoriaId()));
  }

  @Override
  public Map<TipoEstadoDonacion, Long> contarPorEstado() {
    return storage.values().stream()
        .filter(di -> tipo(di) != null)
        .collect(
            Collectors.groupingBy(
                DonacionesIndependientesRepositoryEnMemoria::tipo, Collectors.counting()));
  }

  @Override
  public long contarQuePasaronA(
      TipoEstadoDonacion estado, LocalDateTime desde, LocalDateTime hasta) {
    return storage.values().stream()
        .filter(
            di ->
                di.getHistorial().stream()
                    .anyMatch(
                        cambio ->
                            cambio.getEstadoNuevo() != null
                                && cambio.getEstadoNuevo().getTipo() == estado
                                && !cambio.getTimestamp().isBefore(desde)
                                && cambio.getTimestamp().isBefore(hasta)))
        .count();
  }

  @Override
  public long contarQuePasaronA(
      TipoEstadoDonacion estado,
      LocalDateTime desde,
      LocalDateTime hasta,
      UUID entidadId,
      UUID necesidadId) {
    return storage.values().stream()
        .filter(di -> entidadId.equals(di.getEntidadBeneficiariaId()))
        .filter(di -> necesidadId == null || necesidadId.equals(di.getNecesidadId()))
        .filter(
            di ->
                di.getHistorial().stream()
                    .anyMatch(
                        cambio ->
                            cambio.getEstadoNuevo() != null
                                && cambio.getEstadoNuevo().getTipo() == estado
                                && !cambio.getTimestamp().isBefore(desde)
                                && cambio.getTimestamp().isBefore(hasta)))
        .count();
  }

  @Override
  public Optional<LocalDateTime> primeraFechaRegistro() {
    return storage.values().stream()
        .map(DonacionIndependiente::getFechaRegistro)
        .filter(Objects::nonNull)
        .min(Comparator.naturalOrder());
  }

  @Override
  public Map<YearMonth, Long> contarRegistradasPorMes(YearMonth desde, YearMonth hasta) {
    LocalDateTime inicio = desde.atDay(1).atStartOfDay();
    LocalDateTime fin = hasta.plusMonths(1).atDay(1).atStartOfDay();
    return storage.values().stream()
        .map(DonacionIndependiente::getFechaRegistro)
        .filter(fecha -> fecha != null && enTramo(fecha, inicio, fin))
        .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
  }

  @Override
  public Map<YearMonth, Long> contarQuePasaronAPorMes(
      TipoEstadoDonacion estado, YearMonth desde, YearMonth hasta) {
    LocalDateTime inicio = desde.atDay(1).atStartOfDay();
    LocalDateTime fin = hasta.plusMonths(1).atDay(1).atStartOfDay();
    Map<YearMonth, Long> conteo = new HashMap<>();
    storage
        .values()
        .forEach(
            di ->
                cuandoPasoA(di, estado, inicio, fin).stream()
                    .map(YearMonth::from)
                    .distinct()
                    .forEach(mes -> conteo.merge(mes, 1L, Long::sum)));
    return conteo;
  }

  @Override
  public Map<UUID, Long> contarRegistradasPorCategoria(LocalDateTime desde, LocalDateTime hasta) {
    Map<UUID, Long> conteo = new HashMap<>();
    storage.values().stream()
        .filter(di -> di.getFechaRegistro() != null)
        .filter(di -> enTramo(di.getFechaRegistro(), desde, hasta))
        .forEach(di -> categoriasDe(di).forEach(c -> conteo.merge(c, 1L, Long::sum)));
    return conteo;
  }

  @Override
  public Map<UUID, Long> contarQuePasaronAPorCategoria(
      TipoEstadoDonacion estado, LocalDateTime desde, LocalDateTime hasta) {
    Map<UUID, Long> conteo = new HashMap<>();
    storage.values().stream()
        .filter(di -> !cuandoPasoA(di, estado, desde, hasta).isEmpty())
        .forEach(di -> categoriasDe(di).forEach(c -> conteo.merge(c, 1L, Long::sum)));
    return conteo;
  }

  private static boolean enTramo(LocalDateTime fecha, LocalDateTime desde, LocalDateTime hasta) {
    return !fecha.isBefore(desde) && fecha.isBefore(hasta);
  }

  private static List<LocalDateTime> cuandoPasoA(
      DonacionIndependiente di,
      TipoEstadoDonacion estado,
      LocalDateTime desde,
      LocalDateTime hasta) {
    return di.getHistorial().stream()
        .filter(c -> c.getEstadoNuevo() != null && c.getEstadoNuevo().getTipo() == estado)
        .map(CambioEstado::getTimestamp)
        .filter(t -> enTramo(t, desde, hasta))
        .toList();
  }

  /** Las categorías de los bienes de la donación, sin repetir; ninguna sin el catálogo. */
  private Set<UUID> categoriasDe(DonacionIndependiente di) {
    if (subcategorias == null) {
      return Set.of();
    }
    return di.getItems().stream()
        .map(i -> i.bien().subcategoriaId())
        .filter(Objects::nonNull)
        .map(subcategorias::findById)
        .flatMap(Optional::stream)
        .map(Subcategoria::getCategoriaId)
        .collect(Collectors.toSet());
  }

  @Override
  public List<ConteoAsignadas> contarAsignadas(UUID entidadId, UUID necesidadId) {
    Map<UUID, Map<TipoEstadoDonacion, Long>> conteo = new HashMap<>();
    storage.values().stream()
        .filter(di -> entidadId.equals(di.getEntidadBeneficiariaId()))
        .filter(di -> necesidadId == null || necesidadId.equals(di.getNecesidadId()))
        .filter(di -> tipo(di) != null)
        .forEach(
            di ->
                conteo
                    .computeIfAbsent(di.getNecesidadId(), id -> new HashMap<>())
                    .merge(tipo(di), 1L, Long::sum));
    return conteo.entrySet().stream()
        .flatMap(
            n ->
                n.getValue().entrySet().stream()
                    .map(e -> new ConteoAsignadas(n.getKey(), e.getKey(), e.getValue())))
        .toList();
  }

  @Override
  public PorVencer buscarPorVencer(LocalDate desde, LocalDate hasta) {
    Map<UUID, LocalDate> vencimientoPorDonacion = new HashMap<>();
    storage.values().stream()
        .filter(di -> tipo(di) == TipoEstadoDonacion.EN_DEPOSITO)
        .forEach(
            di ->
                di.getItems().stream()
                    .map(i -> i.bien().bienOriginal().fechaVencimiento())
                    .filter(f -> f != null && !f.isBefore(desde) && !f.isAfter(hasta))
                    .min(Comparator.naturalOrder())
                    .ifPresent(f -> vencimientoPorDonacion.put(di.getId(), f)));
    return vencimientoPorDonacion.entrySet().stream()
        .min(
            Map.Entry.<UUID, LocalDate>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
        .map(e -> new PorVencer(vencimientoPorDonacion.size(), e.getKey(), e.getValue()))
        .orElse(PorVencer.ninguna());
  }

  @Override
  public List<DonacionIndependiente> findEnDeposito() {
    return findAll().stream()
        .filter(d -> d.getEstadoActual().getTipo() == TipoEstadoDonacion.EN_DEPOSITO)
        .toList();
  }

  @Override
  public List<DonacionIndependiente> findCancelablesPorNecesidad(UUID necesidadId) {
    return findAll().stream()
        .filter(d -> necesidadId != null && necesidadId.equals(d.getNecesidadId()))
        .filter(DonacionIndependiente::esCancelable)
        .toList();
  }

  @Override
  public Map<UUID, Integer> sumarCantidadAsignada(Map<UUID, UUID> periodoPorNecesidad) {
    Map<UUID, Integer> sumas = new HashMap<>();
    periodoPorNecesidad.forEach(
        (necesidadId, periodoId) ->
            sumas.put(necesidadId, sumarCantidadAsignada(necesidadId, periodoId)));
    return sumas;
  }

  @Override
  public int sumarCantidadAsignada(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    return storage.values().stream()
        .filter(d -> necesidadId.equals(d.getNecesidadId()))
        .filter(d -> Objects.equals(periodoId, d.getPeriodoId()))
        .mapToInt(DonacionIndependiente::getCantidadContada)
        .sum();
  }

  @Override
  public int sumarCantidadRecibida(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    return storage.values().stream()
        .filter(d -> necesidadId.equals(d.getNecesidadId()))
        .filter(d -> Objects.equals(periodoId, d.getPeriodoId()))
        .filter(
            d ->
                d.getEstadoActual() != null
                    && d.getEstadoActual().getTipo() == TipoEstadoDonacion.ENTREGADA)
        .mapToInt(DonacionIndependiente::getCantidad)
        .sum();
  }

  @Override
  public Map<UUID, Integer> sumarCantidadRecibida(Map<UUID, UUID> periodoPorNecesidad) {
    Map<UUID, Integer> sumas = new HashMap<>();
    periodoPorNecesidad.forEach(
        (necesidadId, periodoId) ->
            sumas.put(necesidadId, sumarCantidadRecibida(necesidadId, periodoId)));
    return sumas;
  }

  @Override
  public Map<UUID, Integer> contarPorEntidadRegistradasDesde(LocalDateTime desde) {
    Map<UUID, Integer> conteo = new HashMap<>();
    storage.values().stream()
        .filter(d -> d.getEntidadBeneficiariaId() != null)
        .filter(
            d ->
                desde == null
                    || (d.getFechaRegistro() != null && d.getFechaRegistro().isAfter(desde)))
        .forEach(d -> conteo.merge(d.getEntidadBeneficiariaId(), 1, Integer::sum));
    return conteo;
  }

  @Override
  public long contarEnEstado(TipoEstadoDonacion estado) {
    return storage.values().stream().filter(d -> d.getEstadoActual().getTipo() == estado).count();
  }

  @Override
  public List<DonacionIndependiente> findDestacadasDesde(LocalDateTime desde, int limite) {
    return destacadasDesde(desde)
        .sorted(
            Comparator.comparing(DonacionIndependiente::getFechaEntrega)
                .reversed()
                .thenComparing(DonacionIndependiente::getId))
        .limit(limite)
        .toList();
  }

  @Override
  public long contarDestacadasDesde(LocalDateTime desde) {
    return destacadasDesde(desde).count();
  }

  private Stream<DonacionIndependiente> destacadasDesde(LocalDateTime desde) {
    return storage.values().stream()
        .filter(d -> d.getEstadoActual().getTipo() == TipoEstadoDonacion.ENTREGADA)
        .filter(d -> d.getFechaEntrega() != null && !d.getFechaEntrega().isBefore(desde));
  }
}
