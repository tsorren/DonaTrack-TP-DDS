package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import java.time.ZoneId;
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

  private MisionDonacionesExitosas(MisionEstado estado, LocalDate fechaUltimoDonacion) {
    super(estado);
    this.fechaUltimoDonacion = fechaUltimoDonacion;
  }

  public static MisionDonacionesExitosas reconstituir(
      MisionEstado estado, LocalDate fechaUltimoDonacion) {
    return new MisionDonacionesExitosas(estado, fechaUltimoDonacion);
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
