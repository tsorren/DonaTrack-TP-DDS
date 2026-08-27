package grupo5.incentivos.models.entities.misiones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class Mision {

  private final UUID id;
  private Integer numeroMision;
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
      throw new ValidationException(ErrorCatalog.MISION_NOMBRE_INVALIDO);
    }
    if (categoria == null) {
      throw new ValidationException(ErrorCatalog.MISION_SIN_CATEGORIA);
    }
    if (objetivo == null || objetivo <= 0) {
      throw new ValidationException(ErrorCatalog.MISION_OBJETIVO_INVALIDO);
    }
    this.id = UUID.randomUUID();
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.categoria = categoria;
    this.objetivo = objetivo;
    this.progresoActual = 0;
    this.completada = false;
  }

  public void setNumeroMision(Integer numeroMision) {
    this.numeroMision = numeroMision;
  }

  public void setInsignia(Insignia insignia) {
    if (insignia == null) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_NULA);
    }
    this.insignia = insignia;
  }

  protected void setProgresoActual(Integer progresoActual) {
    this.progresoActual = progresoActual;
  }

  public void evaluarProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    if (this.completada) {
      return;
    }

    this.progresoActual = calcularNuevoProgreso(donante, evento);

    if (this.progresoActual >= this.objetivo) {
      completar(donante, evento.getFecha());
    }
  }

  protected void completar(DonanteIncentivos donante, LocalDate fecha) {
    this.completada = true;
    this.fechaCompletada = fecha;
    if (this.insignia != null) {
      donante.otorgarInsignia(this.insignia, fecha);
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
