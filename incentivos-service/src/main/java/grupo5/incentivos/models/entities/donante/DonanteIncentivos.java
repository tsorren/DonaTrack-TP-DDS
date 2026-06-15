package grupo5.incentivos.models.entities.donante;

import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.metricas.Metricas;
import grupo5.incentivos.models.entities.misiones.Mision;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonanteIncentivos {

  private Long donanteId;
  private CategoriaDonante categoria;
  private List<CambioCategoria> historialCategorias;
  private List<Mision> misiones;
  private List<Insignia> insignias;
  private Metricas metricas;

  public DonanteIncentivos(Long donanteId) {
    if (donanteId == null) {
      throw new IllegalArgumentException("El ID del donante no puede ser nulo");
    }
    this.donanteId = donanteId;
    this.categoria = CategoriaDonante.COLABORADOR;
    this.historialCategorias = new ArrayList<>();
    this.misiones = new ArrayList<>();
    this.insignias = new ArrayList<>();
    this.metricas = new Metricas();
  }

  public void registrarDonacion(EventoDonacion evento) {
    metricas.registrarDonacion(evento);

    this.misiones.stream()
        .filter(m -> m.getCategoria() == this.categoria && !m.isCompletada())
        .findFirst()
        .ifPresent(m -> m.evaluarProgreso(this, evento));
  }

  public void registrarDonacionExitosa(Long organizacionId) {
    metricas.registrarDonacionExitosa(organizacionId);

    this.misiones.stream()
            .filter(m -> m.getCategoria() == this.categoria && !m.isCompletada())
            .findFirst()
            .ifPresent(m -> m.evaluarProgresoExitoso(this));
  }


  private boolean yaAyudoA(Long organizacionId) {
    return metricas.yaAyudoA(organizacionId);
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
      CategoriaDonante anterior = this.categoria;
      this.categoria = siguienteCategoria();
      this.historialCategorias.add(new CambioCategoria(anterior, this.categoria));
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
