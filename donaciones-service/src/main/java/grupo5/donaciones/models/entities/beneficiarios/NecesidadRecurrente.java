package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.donaciones.models.entities.bienes.SubCategoria;
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
      LocalDate fechaInicioPrimerPeriodo) {

    super(subcategoria, cantidadNecesitada, descripcion);
    this.periodo = periodo;
    this.activa = true;
    this.periodos = new ArrayList<>();

    validarNecesidadRecurrente(fechaInicioPrimerPeriodo);

    this.periodos.add(
        new PeriodoNecesidad(fechaInicioPrimerPeriodo.plus(this.periodo), cantidadNecesitada));
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

  @Override
  public void asignarDonacion(DonacionAsignada donacionAsignada) {
    super.asignarDonacion(donacionAsignada);
    PeriodoNecesidad actual = obtenerPeriodoActual();
    if (actual != null) {
      actual.agregarDonacion(donacionAsignada);
    }
  }

  public void generarNuevoPeriodo() {
    LocalDate nuevaFechaFin =
        periodos.isEmpty()
            ? LocalDate.now().plus(this.periodo)
            : obtenerPeriodoActual().getFechaFin().plus(this.periodo);

    this.periodos.add(new PeriodoNecesidad(nuevaFechaFin, super.getCantidadNecesitada()));
  }

  public boolean hayQueGenerarNuevo() {
    if (this.activa != null && !this.activa) return false;
    if (this.periodos.isEmpty()) return true;

    // crear un período nuevo si "hoy" es posterior a la fecha de vencimiento
    return LocalDate.now().isAfter(obtenerPeriodoActual().getFechaFin());
  }

  public PeriodoNecesidad obtenerPeriodoActual() {
    if (this.periodos.isEmpty()) return null;
    return this.periodos.get(this.periodos.size() - 1);
  }

  public boolean getActiva() {
    return this.activa != null && this.activa;
  }
}
