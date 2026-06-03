package grupo5.donaciones.models.entities.beneficiarios;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodoNecesidad {
  private LocalDate fechaFin;
  private List<DonacionAsignada> donacionesAsignadas;
  private Integer cantidadObjetivo;

  public PeriodoNecesidad(LocalDate fechaFin, Integer cantidadObjetivo) {
    this.fechaFin = fechaFin;
    this.cantidadObjetivo = cantidadObjetivo;
    this.donacionesAsignadas = new ArrayList<>();
  }

  public void agregarDonacion(DonacionAsignada donacion) {
    this.donacionesAsignadas.add(donacion);
  }

  public Integer cantidadAcumulada() {
    return this.donacionesAsignadas.stream().mapToInt(DonacionAsignada::getCantidad).sum();
  }

  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.cantidadObjetivo;
  }

  public boolean estaEnPeriodo(LocalDate fecha) {
    return !fecha.isAfter(this.fechaFin);
  }

  public void finalizo() {}
}
