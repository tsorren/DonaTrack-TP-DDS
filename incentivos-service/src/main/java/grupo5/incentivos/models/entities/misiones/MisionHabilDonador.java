package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import lombok.Getter;

@Getter
public class MisionHabilDonador extends Mision {

  public MisionHabilDonador(CategoriaDonante categoria, Integer cantidadBienesObjetivo) {
    super(
        "Habil Donador",
        "Realiza una donacion con al menos " + cantidadBienesObjetivo + " bienes",
        categoria,
        cantidadBienesObjetivo);
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    Integer cantidadActual = evento.getCantidadBienes() != null ? evento.getCantidadBienes() : 0;

    return Math.max(this.getProgresoActual(), cantidadActual);
  }
}
