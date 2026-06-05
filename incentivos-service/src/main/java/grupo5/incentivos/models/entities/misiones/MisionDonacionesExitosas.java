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
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    if (evento.isExitosa()) {
      return this.getProgresoActual() + 1;
    }
    return this.getProgresoActual();
  }
}
