package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionDonacionesExitosas extends Mision {

  public MisionDonacionesExitosas(CategoriaDonante categoria, Integer donacionesObjetivo) {
    super(
        "Donaciones Exitosas",
        "Logra que " + donacionesObjetivo + " de tus donaciones sean recibidas exitosamente",
        categoria,
        donacionesObjetivo);
  }

  @Override
  public void evaluarProgresoExitoso(DonanteIncentivos donante) {
    if (this.isCompletada()) return;

    this.setProgresoActual(this.getProgresoActual() + 1);

    if (this.getProgresoActual() >= this.getObjetivo()) {
      this.setCompletada(true);
      this.setFechaCompletada(java.time.LocalDate.now());
      if (this.getInsignia() != null) {
        donante.otorgarInsignia(this.getInsignia());
      }
    }
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    return this.getProgresoActual();
  }
}
