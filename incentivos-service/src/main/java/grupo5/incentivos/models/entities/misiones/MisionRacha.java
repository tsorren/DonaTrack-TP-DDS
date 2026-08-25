package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.YearMonth;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionRacha extends Mision {

  private YearMonth ultimoMesDonado;

  public MisionRacha(CategoriaDonante categoria, Integer mesesConsecutivosObjetivo) {
    super(
        "Racha",
        "Realiza una donacion durante " + mesesConsecutivosObjetivo + " meses consecutivos",
        categoria,
        mesesConsecutivosObjetivo);
  }

  public void verificarVigencia(YearMonth mesActual) {
    if (this.isCompletada() || this.ultimoMesDonado == null) {
      return;
    }
    if (mesActual.isAfter(this.ultimoMesDonado.plusMonths(1))) {
      this.ultimoMesDonado = null;
      this.setProgresoActual(0);
    }
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    YearMonth mesEvento = YearMonth.from(evento.getFecha());

    if (this.ultimoMesDonado == null) {

      this.ultimoMesDonado = mesEvento;
      return 1;
    }

    if (mesEvento.equals(this.ultimoMesDonado)) {

      return this.getProgresoActual();
    }

    if (mesEvento.equals(this.ultimoMesDonado.plusMonths(1))) {

      this.ultimoMesDonado = mesEvento;
      return this.getProgresoActual() + 1;
    }

    this.ultimoMesDonado = mesEvento;
    return 1;
  }
}
