package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public abstract class Mision {

  private String nombre;
  private String descripcion;
  private CategoriaDonante categoria;
  private Integer objetivo;
  private Integer progresoActual;
  private boolean completada;
  private LocalDate fechaCompletada;
  private Insignia insignia;

  protected Mision(
      String nombre, String descripcion, CategoriaDonante categoria, Integer objetivo) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new IllegalArgumentException("La mision debe tener un nombre.");
    }
    if (objetivo == null || objetivo <= 0) {
      throw new IllegalArgumentException("El objetivo de la mision debe ser mayor a cero.");
    }
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.categoria = categoria;
    this.objetivo = objetivo;
    this.progresoActual = 0;
    this.completada = false;
  }

  public void evaluarProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    if (this.completada) {
      return;
    }

    this.progresoActual = calcularNuevoProgreso(donante, evento);

    if (this.progresoActual >= this.objetivo) {
      this.completada = true;
      this.fechaCompletada = evento.getFecha();
      if (this.insignia != null) {
        donante.otorgarInsignia(this.insignia);
      }
    }
  }

  protected abstract Integer calcularNuevoProgreso(
      DonanteIncentivos donante, EventoDonacion evento);

  public int getPorcentajeProgreso() {
    if (this.objetivo == 0) return 100;
    return Math.min(100, (this.progresoActual * 100) / this.objetivo);
  }

  public int getDistanciaAlObjetivo() {
    return Math.max(0, this.objetivo - this.progresoActual);
  }

  public boolean fueCompletadaEnMes(int anio, int mes) {
    if (!this.completada || this.fechaCompletada == null) return false;
    return this.fechaCompletada.getYear() == anio && this.fechaCompletada.getMonthValue() == mes;
  }

  public void evaluarProgresoExitoso(DonanteIncentivos donante) {
    // por defecto no hace nada
  }
}
