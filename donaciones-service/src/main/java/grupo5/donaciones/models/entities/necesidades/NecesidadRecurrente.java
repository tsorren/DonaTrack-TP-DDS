package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
  private Period periodo;
  private List<PeriodoNecesidad> periodos;
  private Boolean activa;

  public NecesidadRecurrente(
      Subcategoria subcategoria,
      Integer cantidadNecesitada,
      String descripcion,
      Period periodo,
      LocalDate fechaInicio) {
    super(subcategoria, cantidadNecesitada, descripcion);
    if (periodo == null)
      throw new ValidationException(ErrorCatalog.NECESIDAD_RECURRENTE_SIN_PERIODO);
    if (fechaInicio == null) throw new ValidationException(ErrorCatalog.FECHA_INICIO_NULA);

    this.periodo = periodo;
    this.activa = true;
    this.periodos = new ArrayList<>();

    validarNecesidadRecurrente(fechaInicio);
    this.periodos.add(new PeriodoNecesidad(fechaInicio.plus(this.periodo), cantidadNecesitada));
  }

  private void validarNecesidadRecurrente(LocalDate fechaInicio) {
    if (periodo == null) {
      throw new ValidationException(ErrorCatalog.NECESIDAD_RECURRENTE_SIN_PERIODO);
    }
    if (fechaInicio == null) {
      throw new ValidationException(ErrorCatalog.FECHA_INICIO_NULA);
    }
    if (fechaInicio.isAfter(LocalDate.now(ZoneId.systemDefault()))) {
      throw new ValidationException(ErrorCatalog.FECHA_INICIO_FUTURA);
    }
  }

  public PeriodoNecesidad obtenerPeriodoActual() {
    if (this.periodos.isEmpty()) return null;
    return this.periodos.get(this.periodos.size() - 1);
  }

  public void asignarDonacion(DonacionIndependiente donacionAsignada) {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual == null) {
      throw new BusinessStateException(ErrorCatalog.SIN_PERIODO_ACTIVO);
    }
    actual.asignarDonacion(donacionAsignada);
  }

  @Override
  public void quitarDonacion(DonacionIndependiente donacion) {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual != null) {
      actual.quitarDonacion(donacion);
    }
  }

  @Override
  public Integer cantidadAcumulada() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    return actual != null ? actual.cantidadAcumulada() : 0;
  }

  @Override
  public boolean estaSatisfecha() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    return actual != null && actual.estaSatisfecha();
  }

  public boolean hayQueGenerarNuevo(LocalDate fechaActual) {
    if (this.activa != null && !this.activa) return false;
    if (this.periodos.isEmpty()) return true;

    // crear un período nuevo si "hoy" es posterior a la fecha de vencimiento
    return !obtenerPeriodoActual().estaEnPeriodo(fechaActual);
  }

  public void generarNuevoPeriodo() {
    LocalDate nuevaFechaFin =
        periodos.isEmpty()
            ? LocalDate.now(ZoneId.systemDefault()).plus(this.periodo)
            : obtenerPeriodoActual().getFechaFin().plus(this.periodo);

    this.periodos.add(new PeriodoNecesidad(nuevaFechaFin, super.getCantidadNecesitada()));
  }

  public boolean getActiva() {
    return this.activa != null && this.activa;
  }
}
