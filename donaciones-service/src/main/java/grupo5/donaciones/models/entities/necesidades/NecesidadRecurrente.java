package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class NecesidadRecurrente extends Necesidad {
  private final Period periodo;

  @Getter(AccessLevel.NONE)
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
    this.periodos.add(new PeriodoNecesidad(fechaInicio.plus(this.periodo), cantidadNecesitada));
  }

  /** Constructor de reconstitución para el adaptador JPA */
  public NecesidadRecurrente(
      DatosNecesidad datos, Period periodo, Boolean activa, List<PeriodoNecesidad> periodos) {
    super(datos);
    this.periodo = periodo;
    this.activa = activa == null || activa;
    this.periodos = periodos != null ? new ArrayList<>(periodos) : new ArrayList<>();
  }

  public void actualizarPeriodos(List<PeriodoNecesidad> periodos) {
    this.periodos = periodos != null ? new ArrayList<>(periodos) : new ArrayList<>();
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

  public List<PeriodoNecesidad> getPeriodos() {
    return List.copyOf(periodos);
  }

  public PeriodoNecesidad obtenerPeriodoActual() {
    if (this.periodos == null || this.periodos.isEmpty()) return null;
    return this.periodos.get(this.periodos.size() - 1);
  }

  @Override
  public DestinoAsignacion destinoDeAsignacion() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual == null) {
      throw new BusinessStateException(ErrorCatalog.SIN_PERIODO_ACTIVO);
    }
    return new DestinoAsignacion(getId(), actual.id(), getEntidadId());
  }

  @Override
  public UUID periodoActualId() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    return actual != null ? actual.id() : null;
  }

  /** Se satisface dentro del período actual, según su cantidad objetivo. */
  @Override
  public boolean estaSatisfechaCon(int cantidadAcumulada) {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    return actual != null && actual.estaSatisfechaCon(cantidadAcumulada);
  }

  public boolean hayQueGenerarNuevo(LocalDate fechaActual) {
    if (this.activa != null && !this.activa) return false;
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual == null) return true;

    // crear un período nuevo si "hoy" es posterior a la fecha de vencimiento
    return !actual.estaEnPeriodo(fechaActual);
  }

  /** {@code acumuladoDelPeriodoActual}: lo asignado al período que se cierra. */
  public boolean renovarPeriodoSiCorresponde(LocalDate fechaActual, int acumuladoDelPeriodoActual) {
    if (!hayQueGenerarNuevo(fechaActual)) {
      return false;
    }
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual != null) {
      actual.finalizo(acumuladoDelPeriodoActual);
    }
    generarNuevoPeriodo();
    return true;
  }

  public void generarNuevoPeriodo() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    LocalDate nuevaFechaFin =
        (actual == null || actual.fechaFin() == null)
            ? LocalDate.now(ZoneId.systemDefault()).plus(this.periodo)
            : actual.fechaFin().plus(this.periodo);

    this.periodos.add(new PeriodoNecesidad(nuevaFechaFin, super.getCantidadNecesitada()));
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
  public TipoNecesidad getTipoNecesidad() {
    return TipoNecesidad.RECURRENTE;
  }

  /** Uno de los tres largos, o {@code null} si se creó antes de cerrarlos con otro largo. */
  public Periodicidad getPeriodicidad() {
    return Periodicidad.de(periodo);
  }

  /** Una recurrente está vigente o dada de baja: cada período tiene su propio resultado. */
  @Override
  public EstadoNecesidad estado(int recibido, LocalDate hoy) {
    return getActiva() ? EstadoNecesidad.VIGENTE : EstadoNecesidad.DADA_DE_BAJA;
  }

  @Override
  public LocalDate finDelPeriodoActual() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    return actual != null ? actual.fechaFin() : null;
  }

  /** Empieza donde terminó el anterior; el primero, el día de la publicación. */
  public LocalDate inicioDe(PeriodoNecesidad unPeriodo) {
    int i = periodos.indexOf(unPeriodo);
    return i <= 0 ? getFechaInicio() : periodos.get(i - 1).fechaFin();
  }

  /**
   * El cambio de cantidad vale también para el período en curso (ADR 20261102 punto 5): su objetivo
   * pasa a la cantidad nueva. Los períodos cerrados no cambian.
   */
  @Override
  public void actualizarCantidadNecesitada(Integer cantidadNecesitada) {
    super.actualizarCantidadNecesitada(cantidadNecesitada);
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual != null) {
      periodos.set(
          periodos.size() - 1,
          new PeriodoNecesidad(actual.id(), actual.fechaFin(), cantidadNecesitada));
    }
  }

  @Override
  public boolean muestraUltimoCambio() {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    return super.muestraUltimoCambio()
        && (actual == null || !getFechaUltimoCambio().isBefore(inicioDe(actual)));
  }
}
