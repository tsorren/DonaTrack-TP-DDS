package grupo5.incentivos.models.entities.donante;

import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonanteIncentivos {

  private Long donanteId;
  private CategoriaDonante categoria;
  private List<Mision> misiones;
  private List<Insignia> insignias;

  private Integer totalDonacionesHistoricas;
  private Integer totalOrganizacionesAyudadas;
  private Integer totalDonacionesExitosas;
  private LocalDate ultimaDonacion;

  public DonanteIncentivos(Long donanteId) {
    if (donanteId == null) {
      throw new IllegalArgumentException("El ID del donante no puede ser nulo");
    }
    this.donanteId = donanteId;
    this.categoria = CategoriaDonante.COLABORADOR;
    this.misiones = new ArrayList<>();
    this.insignias = new ArrayList<>();
    this.totalDonacionesHistoricas = 0;
    this.totalOrganizacionesAyudadas = 0;
    this.totalDonacionesExitosas = 0;
  }

  public void registrarDonacion(EventoDonacion evento) {
    this.totalDonacionesHistoricas++;
    this.ultimaDonacion = evento.getFecha();

    if (evento.isExitosa()) {
      this.totalDonacionesExitosas++;
    }

    if (evento.getOrganizacionId() != null && !this.yaAyudoA(evento.getOrganizacionId())) {
      this.totalOrganizacionesAyudadas++;
    }

    this.misiones.stream()
        .filter(m -> !m.isCompletada())
        .forEach(m -> m.evaluarProgreso(this, evento));
  }

  private boolean yaAyudoA(Long organizacionId) {
    // implementar
    return false;
  }

  public void otorgarInsignia(Insignia insignia) {
    if (insignia == null) {
      throw new IllegalArgumentException("La insignia no puede ser nula");
    }
    this.insignias.add(insignia);
  }

  public boolean intentarAscenso() {
    boolean todasCompletadasEnCategoria =
        this.misiones.stream()
            .filter(m -> m.getCategoria() == this.categoria)
            .allMatch(Mision::isCompletada);

    if (todasCompletadasEnCategoria && this.categoria != CategoriaDonante.TRANSFORMADOR) {
      this.categoria = siguienteCategoria();
      return true;
    }
    return false;
  }

  private CategoriaDonante siguienteCategoria() {
    return switch (this.categoria) {
      case COLABORADOR -> CategoriaDonante.SOSTENEDOR;
      case SOSTENEDOR -> CategoriaDonante.TRANSFORMADOR;
      case TRANSFORMADOR -> CategoriaDonante.TRANSFORMADOR;
    };
  }

  public Mision getMisionActiva() {
    return this.misiones.stream()
        .filter(m -> m.getCategoria() == this.categoria && !m.isCompletada())
        .findFirst()
        .orElse(null);
  }

  public long misionesCompletadasEnMes(int anio, int mes) {
    return this.misiones.stream()
        .filter(m -> m.isCompletada() && m.fueCompletadaEnMes(anio, mes))
        .count();
  }
}
