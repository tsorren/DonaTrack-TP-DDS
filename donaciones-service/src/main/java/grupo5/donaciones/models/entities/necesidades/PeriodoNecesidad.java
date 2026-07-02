package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public record PeriodoNecesidad(
    LocalDate fechaFin,
    List<DonacionIndependiente> donacionesAsignadas,
    Integer cantidadObjetivo,
    NecesidadRecurrente necesidadRecurrente)
    implements Asignable {

  private static final Logger logger = Logger.getLogger(PeriodoNecesidad.class.getName());

  public PeriodoNecesidad(LocalDate fechaFin, Integer cantidadObjetivo) {
    this(fechaFin, new ArrayList<>(), cantidadObjetivo, null);
  }

  public PeriodoNecesidad asignarDonacion(DonacionIndependiente donacion) {
    if (donacion == null) throw new ValidationException(ErrorCatalog.PERIODO_DONACION_NULA);
    List<DonacionIndependiente> nuevasDonaciones = new ArrayList<>(this.donacionesAsignadas);
    nuevasDonaciones.add(donacion);
    return new PeriodoNecesidad(
        this.fechaFin, nuevasDonaciones, this.cantidadObjetivo, this.necesidadRecurrente);
  }

  public PeriodoNecesidad quitarDonacion(DonacionIndependiente donacion) {
    List<DonacionIndependiente> nuevasDonaciones = new ArrayList<>(this.donacionesAsignadas);
    nuevasDonaciones.remove(donacion);
    return new PeriodoNecesidad(
        this.fechaFin, nuevasDonaciones, this.cantidadObjetivo, this.necesidadRecurrente);
  }

  public PeriodoNecesidad conNecesidadRecurrente(NecesidadRecurrente necesidadRecurrente) {
    return new PeriodoNecesidad(
        this.fechaFin, this.donacionesAsignadas, this.cantidadObjetivo, necesidadRecurrente);
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
    }
  }

  @Override
  public Necesidad obtenerNecesidad() {
    return necesidadRecurrente;
  }
}
