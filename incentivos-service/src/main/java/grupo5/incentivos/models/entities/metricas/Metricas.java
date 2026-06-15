package grupo5.incentivos.models.entities.metricas;

import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Metricas {

  private Integer totalDonacionesHistoricas;
  private Integer totalDonacionesExitosas;
  private LocalDate ultimaDonacion;
  private List<EventoDonacion> historialDonaciones;
  private Set<Long> organizacionesAyudadas;

  public Metricas() {
    this.totalDonacionesHistoricas = 0;
    this.totalDonacionesExitosas = 0;
    this.historialDonaciones = new ArrayList<>();
    this.organizacionesAyudadas = new HashSet<>();
  }

  public Integer getTotalOrganizacionesAyudadas() {
    return organizacionesAyudadas.size();
  }

  public void registrarDonacion(EventoDonacion evento) {
    this.totalDonacionesHistoricas++;
    this.ultimaDonacion = evento.getFecha();
    this.historialDonaciones.add(evento);
  }

  public void registrarDonacionExitosa(Long organizacionId) {
    this.totalDonacionesExitosas++;
    if (organizacionId != null && !this.yaAyudoA(organizacionId)) {
      this.registrarOrganizacionAyudada(organizacionId);
    }
  }

  public boolean yaAyudoA(Long organizacionId) {
    return organizacionesAyudadas.contains(organizacionId);
  }

  public void registrarOrganizacionAyudada(Long organizacionId) {
    organizacionesAyudadas.add(organizacionId);
  }

  public Map<YearMonth, Long> donacionesPorPeriodo() {
    return historialDonaciones.stream()
        .collect(Collectors.groupingBy(e -> YearMonth.from(e.getFecha()), Collectors.counting()));
  }

  public long donacionesEnMes(YearMonth periodo) {
    return historialDonaciones.stream()
        .filter(e -> YearMonth.from(e.getFecha()).equals(periodo))
        .count();
  }

  public long donacionesMesActual() {
    return donacionesEnMes(YearMonth.now());
  }

  public long donacionesMesAnterior() {
    return donacionesEnMes(YearMonth.now().minusMonths(1));
  }
}
