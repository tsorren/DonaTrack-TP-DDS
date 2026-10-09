package grupo5.incentivos.models.entities.metricas;

import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Metricas {

  private Integer totalDonacionesHistoricas;
  private Integer totalDonacionesExitosas;
  private LocalDate ultimaDonacion;

  // Cantidad de donaciones por mes. Reemplaza al historial completo de eventos: el agregado solo
  // necesita contar, asi que crece con los meses de actividad y no con cada donacion.
  @Getter(AccessLevel.NONE)
  private final Map<YearMonth, Long> conteoPorPeriodo;

  private Set<UUID> organizacionesAyudadas;

  public Metricas() {
    this.totalDonacionesHistoricas = 0;
    this.totalDonacionesExitosas = 0;
    this.conteoPorPeriodo = new TreeMap<>();
    this.organizacionesAyudadas = new HashSet<>();
  }

  /** Reconstituye las métricas persistidas sin pasar por las reglas de registro. */
  public static Metricas reconstituir(
      Integer totalDonacionesHistoricas,
      Integer totalDonacionesExitosas,
      LocalDate ultimaDonacion,
      Map<YearMonth, Long> conteoPorPeriodo,
      Set<UUID> organizacionesAyudadas) {
    Metricas metricas = new Metricas();
    metricas.totalDonacionesHistoricas = totalDonacionesHistoricas;
    metricas.totalDonacionesExitosas = totalDonacionesExitosas;
    metricas.ultimaDonacion = ultimaDonacion;
    if (conteoPorPeriodo != null) {
      metricas.conteoPorPeriodo.putAll(conteoPorPeriodo);
    }
    if (organizacionesAyudadas != null) {
      metricas.organizacionesAyudadas.addAll(organizacionesAyudadas);
    }
    return metricas;
  }

  public Integer getTotalOrganizacionesAyudadas() {
    return organizacionesAyudadas.size();
  }

  public void registrarDonacion(EventoDonacion evento) {
    this.totalDonacionesHistoricas++;
    this.ultimaDonacion = evento.getFecha();
    this.conteoPorPeriodo.merge(YearMonth.from(evento.getFecha()), 1L, Long::sum);
  }

  public void registrarDonacionExitosa(UUID organizacionId) {
    this.totalDonacionesExitosas++;
    if (organizacionId != null && !this.yaAyudoA(organizacionId)) {
      this.registrarOrganizacionAyudada(organizacionId);
    }
  }

  public boolean yaAyudoA(UUID organizacionId) {
    return organizacionesAyudadas.contains(organizacionId);
  }

  public void registrarOrganizacionAyudada(UUID organizacionId) {
    organizacionesAyudadas.add(organizacionId);
  }

  public Map<YearMonth, Long> donacionesPorPeriodo() {
    return Collections.unmodifiableMap(new TreeMap<>(conteoPorPeriodo));
  }

  public Set<UUID> getOrganizacionesAyudadas() {
    return Set.copyOf(this.organizacionesAyudadas);
  }

  public long donacionesEnMes(YearMonth periodo) {
    return conteoPorPeriodo.getOrDefault(periodo, 0L);
  }

  public long donacionesMesActual() {
    return donacionesEnMes(YearMonth.now(ZoneId.systemDefault()));
  }

  public long donacionesMesAnterior() {
    return donacionesEnMes(YearMonth.now(ZoneId.systemDefault()).minusMonths(1));
  }
}
