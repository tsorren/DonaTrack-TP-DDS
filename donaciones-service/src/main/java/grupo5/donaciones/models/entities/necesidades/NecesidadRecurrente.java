package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class NecesidadRecurrente extends Necesidad {
  private Period periodo;
  private List<PeriodoNecesidad> periodos;
  private Boolean activa;

  public NecesidadRecurrente(
      UUID subcategoriaId,
      Integer cantidadNecesitada,
      String descripcion,
      Period periodo,
      LocalDate fechaInicio) {
    super(subcategoriaId, cantidadNecesitada, descripcion);
    if (periodo == null)
      throw new ValidationException(ErrorCatalog.NECESIDAD_RECURRENTE_SIN_PERIODO);
    if (fechaInicio == null) throw new ValidationException(ErrorCatalog.FECHA_INICIO_NULA);

    this.periodo = periodo;
    this.activa = true;
    this.periodos = new ArrayList<>();

    validarNecesidadRecurrente(fechaInicio);
    this.periodos.add(
        new PeriodoNecesidad(fechaInicio.plus(this.periodo), List.of(), cantidadNecesitada, this));
  }

  private void validarNecesidadRecurrente(LocalDate fechaInicio) {
    if (periodo == null) {
      throw new ValidationException(ErrorCatalog.NECESIDAD_RECURRENTE_SIN_PERIODO);
    }
    if (fechaInicio == null) {
      throw new ValidationException(ErrorCatalog.FECHA_INICIO_NULA);
    }
    if (fechaInicio.isAfter(LocalDate.now(ZoneId.of("UTC")))) {
      throw new ValidationException(ErrorCatalog.FECHA_INICIO_FUTURA);
    }
  }

  @Override
  public List<DonacionIndependiente> getDonacionesAsignadas() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    return actual != null && actual.donacionesAsignadas() != null
        ? actual.donacionesAsignadas()
        : List.of();
  }

  public PeriodoNecesidad obtenerPeriodoActual() {
    if (this.periodos == null || this.periodos.isEmpty()) return null;
    return this.periodos.get(this.periodos.size() - 1);
  }

  public void asignarDonacion(DonacionIndependiente donacionAsignada) {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual == null) {
      throw new BusinessStateException(ErrorCatalog.SIN_PERIODO_ACTIVO);
    }
    PeriodoNecesidad nuevoActual = actual.asignarDonacion(donacionAsignada);
    this.periodos.set(this.periodos.size() - 1, nuevoActual);
  }

  @Override
  public void quitarDonacion(DonacionIndependiente donacion) {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual != null) {
      PeriodoNecesidad nuevoActual = actual.quitarDonacion(donacion);
      this.periodos.set(this.periodos.size() - 1, nuevoActual);
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
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual == null) return true;

    // crear un período nuevo si "hoy" es posterior a la fecha de vencimiento
    return !actual.estaEnPeriodo(fechaActual);
  }

  public boolean renovarPeriodoSiCorresponde(LocalDate fechaActual) {
    if (!hayQueGenerarNuevo(fechaActual)) {
      return false;
    }
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual != null) {
      actual.finalizo();
    }
    generarNuevoPeriodo();
    return true;
  }

  public void generarNuevoPeriodo() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    LocalDate nuevaFechaFin =
        (actual == null || actual.fechaFin() == null)
            ? LocalDate.now(ZoneId.of("UTC")).plus(this.periodo)
            : actual.fechaFin().plus(this.periodo);

    this.periodos.add(
        new PeriodoNecesidad(nuevaFechaFin, List.of(), super.getCantidadNecesitada(), this));
  }

  public boolean getActiva() {
    return this.activa != null && this.activa;
  }

  @Override
  public boolean isActiva() {
    return getActiva();
  }

  @Override
  public void desactivar() {
    this.activa = false;
  }

  @Override
  public int contarDonacionesAsignadasDesde(LocalDateTime desde) {
    if (this.periodos == null || desde == null) return 0;
    return this.periodos.stream()
        .filter(p -> p.donacionesAsignadas() != null)
        .flatMap(p -> p.donacionesAsignadas().stream())
        .filter(d -> d.getFechaRegistro() != null && d.getFechaRegistro().isAfter(desde))
        .mapToInt(d -> 1)
        .sum();
  }

  @Override
  public TipoNecesidad getTipoNecesidad() {
    return TipoNecesidad.RECURRENTE;
  }

  @Override
  public NecesidadDTO toDTO() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    LocalDate fechaFin = actual != null ? actual.fechaFin() : null;
    return super.toDTO(fechaFin);
  }
}
