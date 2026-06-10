package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodoNecesidad {
  private static final Logger logger = Logger.getLogger(PeriodoNecesidad.class.getName());

  private LocalDate fechaFin;
  private List<DonacionAsignada> donacionesAsignadas;
  private Integer cantidadObjetivo;

  public PeriodoNecesidad(LocalDate fechaFin, Integer cantidadObjetivo) {
    this.fechaFin = fechaFin;
    this.cantidadObjetivo = cantidadObjetivo;
    this.donacionesAsignadas = new ArrayList<>();
  }

  public void agregarDonacion(DonacionAsignada donacion) {
    if (donacion == null) throw new ValidationException(ErrorCatalog.PERIODO_DONACION_NULA);
    this.donacionesAsignadas.add(donacion);
  }

  public void quitarDonacion(DonacionAsignada donacion) {
    this.donacionesAsignadas.remove(donacion);
  }

  public Integer cantidadAcumulada() {
    return this.donacionesAsignadas.stream().mapToInt(DonacionAsignada::getCantidad).sum();
  }

  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.cantidadObjetivo;
  }

  public boolean estaEnPeriodo(LocalDate fecha) {
    if (this.fechaFin == null) {
      return true;
    }
    return !fecha.isAfter(this.fechaFin);
  }

  public void finalizo() {
    if (!this.estaSatisfecha()) {
      logger.warning("El período cerró sin alcanzar la meta de " + this.cantidadObjetivo);
      // Idealmente acá se dispararía un evento de notificación
    }
  }
}
