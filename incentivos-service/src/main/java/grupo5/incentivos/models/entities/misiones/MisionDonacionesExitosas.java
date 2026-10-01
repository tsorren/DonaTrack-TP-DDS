package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;

@Getter
public class MisionDonacionesExitosas extends Mision {

  private LocalDate fechaUltimoDonacion;

  public MisionDonacionesExitosas(CategoriaDonante categoria, Integer donacionesObjetivo) {
    super(
        "Donaciones Exitosas",
        "Logra que " + donacionesObjetivo + " de tus donaciones sean recibidas exitosamente",
        categoria,
        donacionesObjetivo);
  }

  private MisionDonacionesExitosas(
      UUID id,
      Integer numeroMision,
      String nombre,
      String descripcion,
      CategoriaDonante categoria,
      Integer objetivo,
      Integer progresoActual,
      boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia,
      LocalDate fechaUltimoDonacion) {
    super(
        id,
        numeroMision,
        nombre,
        descripcion,
        categoria,
        objetivo,
        progresoActual,
        completada,
        fechaCompletada,
        insignia);
    this.fechaUltimoDonacion = fechaUltimoDonacion;
  }

  public static MisionDonacionesExitosas reconstituir(
      UUID id,
      Integer numeroMision,
      String nombre,
      String descripcion,
      CategoriaDonante categoria,
      Integer objetivo,
      Integer progresoActual,
      boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia,
      LocalDate fechaUltimoDonacion) {
    return new MisionDonacionesExitosas(
        id,
        numeroMision,
        nombre,
        descripcion,
        categoria,
        objetivo,
        progresoActual,
        completada,
        fechaCompletada,
        insignia,
        fechaUltimoDonacion);
  }

  @Override
  public void evaluarProgresoExitoso(DonanteIncentivos donante) {
    if (this.isCompletada()) return;

    this.setProgresoActual(this.getProgresoActual() + 1);

    if (this.getProgresoActual() >= this.getObjetivo()) {
      LocalDate fecha =
          (fechaUltimoDonacion != null)
              ? fechaUltimoDonacion
              : LocalDate.now(ZoneId.systemDefault());
      completar(donante, fecha);
    }
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    this.fechaUltimoDonacion = evento.getFecha();
    return this.getProgresoActual();
  }
}
