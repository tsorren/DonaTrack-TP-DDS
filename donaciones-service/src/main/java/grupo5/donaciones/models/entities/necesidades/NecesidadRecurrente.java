package grupo5.donaciones.models.entities.necesidades;

import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.time.Period;
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
      SubCategoria subcategoria,
      Integer cantidadNecesitada,
      String descripcion,
      Period periodo,
      LocalDate fechaInicio) {
    super(subcategoria, cantidadNecesitada, descripcion);
    if (periodo == null) throw new IllegalArgumentException("Debe tener un período definido.");
    if (fechaInicio == null)
      throw new IllegalArgumentException("La fecha de inicio no puede ser nula.");

    this.periodo = periodo;
    this.activa = true;
    this.periodos = new ArrayList<>();

    validarNecesidadRecurrente(fechaInicio);
    this.periodos.add(new PeriodoNecesidad(fechaInicio.plus(this.periodo), cantidadNecesitada));
  }

  private void validarNecesidadRecurrente(LocalDate fechaInicio) {
    if (periodo == null) {
      throw new IllegalArgumentException("La necesidad recurrente debe tener un período definido.");
    }
    if (fechaInicio == null) {
      throw new IllegalArgumentException("La fecha de inicio del período no puede ser nula.");
    }
    if (fechaInicio.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("La fecha de inicio del período no puede ser futura.");
    }
  }

  public PeriodoNecesidad obtenerPeriodoActual() {
    if (this.periodos.isEmpty()) return null;
    return this.periodos.get(this.periodos.size() - 1);
  }

  public void asignarDonacion(DonacionIndependiente donacionAsignada) {
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual == null) {
      throw new IllegalStateException("No existe un período activo.");
    }
    actual.agregarDonacion(donacionAsignada);
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

  public boolean hayQueGenerarNuevo() {
    if (this.activa != null && !this.activa) return false;
    if (this.periodos.isEmpty()) return true;

    // crear un período nuevo si "hoy" es posterior a la fecha de vencimiento
    return !obtenerPeriodoActual().estaEnPeriodo(LocalDate.now());
  }

  public void generarNuevoPeriodo() {
    LocalDate nuevaFechaFin =
        periodos.isEmpty()
            ? LocalDate.now().plus(this.periodo)
            : obtenerPeriodoActual().getFechaFin().plus(this.periodo);

    this.periodos.add(new PeriodoNecesidad(nuevaFechaFin, super.getCantidadNecesitada()));
  }

  public boolean getActiva() {

    return this.activa != null && this.activa;
  }
}
