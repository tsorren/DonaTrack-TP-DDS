package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.YearMonth;
import lombok.Getter;

@Getter
public class MisionRacha extends Mision {

  private YearMonth ultimoMesDonado;

  public MisionRacha(CategoriaDonante categoria, Integer mesesConsecutivosObjetivo) {
    super(
        "Racha",
        "Realiza una donacion durante " + mesesConsecutivosObjetivo + " meses consecutivos",
        categoria,
        mesesConsecutivosObjetivo);
  }

  @Override
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

    // ultimoMesDonado es siempre el mes mas reciente de la racha; el primero se deriva
    // a partir del progreso actual, ya que la racha es siempre un rango contiguo de meses.
    YearMonth primerMesRacha = this.ultimoMesDonado.minusMonths(this.getProgresoActual() - 1);

    // Mes ya contemplado en la racha actual (incluye duplicados): no suma ni resta.
    if (!mesEvento.isBefore(primerMesRacha) && !mesEvento.isAfter(this.ultimoMesDonado)) {
      return this.getProgresoActual();
    }

    // Extiende la racha hacia adelante (caso normal).
    if (mesEvento.equals(this.ultimoMesDonado.plusMonths(1))) {
      this.ultimoMesDonado = mesEvento;
      return this.getProgresoActual() + 1;
    }

    // Extiende la racha hacia atras (donacion de un mes anterior que llega demorada).
    // ultimoMesDonado no cambia: sigue siendo el mes mas reciente de la racha.
    if (mesEvento.equals(primerMesRacha.minusMonths(1))) {
      return this.getProgresoActual() + 1;
    }

    // Donacion vieja que no conecta con la racha actual: se ignora, no la rompe.
    if (mesEvento.isBefore(primerMesRacha)) {
      return this.getProgresoActual();
    }

    // Hay un salto hacia adelante (se perdio algun mes en el medio): arranca una racha nueva.
    this.ultimoMesDonado = mesEvento;
    return 1;
  }
}
