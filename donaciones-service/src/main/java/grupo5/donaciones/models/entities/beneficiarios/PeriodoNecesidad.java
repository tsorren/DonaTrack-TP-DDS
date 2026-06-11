package grupo5.donaciones.models.entities.beneficiarios;

// Aunque no se usa directamente aquí,
// es buena práctica mantener el
// paquete
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodoNecesidad {
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
  private List<DonacionAsignada> donacionesAsignadas;
  private Integer cantidadNecesitadaObjetivo; // Para saber el objetivo de este periodo

  public PeriodoNecesidad(
      LocalDate fechaInicio, LocalDate fechaFin, Integer cantidadNecesitadaObjetivo) {
    this.fechaInicio = fechaInicio;
    this.fechaFin = fechaFin;
    this.cantidadNecesitadaObjetivo = cantidadNecesitadaObjetivo;
    this.donacionesAsignadas = new ArrayList<>();

    validarPeriodoNecesidad();
  }

  private void validarPeriodoNecesidad() {
    if (this.fechaInicio == null) {
      throw new IllegalArgumentException("La fecha de inicio del período no puede ser nula.");
    }
    if (this.fechaFin == null) {
      throw new IllegalArgumentException("La fecha de fin del período no puede ser nula.");
    }
    if (this.fechaInicio.isAfter(this.fechaFin)) {
      throw new IllegalArgumentException(
          "La fecha de inicio no puede ser posterior a la fecha de fin.");
    }
    if (this.cantidadNecesitadaObjetivo == null || this.cantidadNecesitadaObjetivo <= 0) {
      throw new IllegalArgumentException(
          "La cantidad necesitada objetivo para el período debe ser mayor a cero.");
    }
  }

  public void asignarDonacion(DonacionAsignada donacionAsignada) {
    if (donacionAsignada == null) {
      throw new IllegalArgumentException("La donación asignada no puede ser nula.");
    }
    // Opcional: Validar si la fecha de asignación de la donación cae dentro de este período
    if (!estaEnPeriodo(donacionAsignada.getFechaAsignacion())) {
      throw new IllegalArgumentException("La donación no corresponde a este período.");
    }

    if (this.donacionesAsignadas.contains(donacionAsignada)) {
      throw new IllegalArgumentException("La donación ya fue asignada a este período.");
    }

    this.donacionesAsignadas.add(donacionAsignada);
  }

  public void quitarDonacion(DonacionAsignada donacionAsignada) {
    if (!this.donacionesAsignadas.contains(donacionAsignada)) {
      throw new IllegalArgumentException("La donación no pertenece a este período.");
    }
    this.donacionesAsignadas.remove(donacionAsignada);
  }

  public Integer cantidadAcumulada() {
    return this.donacionesAsignadas.stream().mapToInt(DonacionAsignada::getCantidad).sum();
  }

  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.cantidadNecesitadaObjetivo;
  }

  public boolean estaEnPeriodo(LocalDate fecha) {
    if (fecha == null) {
      throw new IllegalArgumentException("La fecha a verificar no puede ser nula.");
    }
    // Inicio <= fecha < fin (el fin no está incluido, es el inicio del siguiente)
    return (fecha.isAfter(fechaInicio) || fecha.isEqual(fechaInicio)) && fecha.isBefore(fechaFin);
  }
}
