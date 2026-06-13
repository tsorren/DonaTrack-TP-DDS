package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodoNecesidad implements Asignable {
  private static final Logger logger = Logger.getLogger(PeriodoNecesidad.class.getName());

  private LocalDate fechaFin;
  private List<DonacionIndependiente> donacionesAsignadas;
  private Integer cantidadObjetivo;
  private NecesidadRecurrente necesidadRecurrente;

  public PeriodoNecesidad(LocalDate fechaFin, Integer cantidadObjetivo) {
    this.fechaFin = fechaFin;
    this.cantidadObjetivo = cantidadObjetivo;
    this.donacionesAsignadas = new ArrayList<>();
  }

  public void agregarDonacion(DonacionIndependiente donacion) {
    if (donacion == null) throw new ValidationException(ErrorCatalog.PERIODO_DONACION_NULA);
    this.donacionesAsignadas.add(donacion);
  }

  public void quitarDonacion(DonacionIndependiente donacion) {
    this.donacionesAsignadas.remove(donacion);
  }

  public Integer cantidadAcumulada() {
    return this.donacionesAsignadas.stream().mapToInt(DonacionIndependiente::getCantidad).sum();
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
      String msj = "El período cerró sin alcanzar la meta de " + this.cantidadObjetivo;
      logger.warning(msj);
      // Idealmente acá se dispararía un evento de notificación
    }
  }

  @Override
  public Necesidad obtenerNecesidad() {
    return necesidadRecurrente;
  }
}
