package grupo5.incentivos.models.entities.ranking;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntradaRanking {

  private int posicion;
  private Long donanteId;
  private String nombreDonante;
  private long misionesCompletadas;

  public EntradaRanking(
      int posicion, Long donanteId, String nombreDonante, long misionesCompletadas) {
    this.posicion = posicion;
    this.donanteId = donanteId;
    this.nombreDonante = nombreDonante;
    this.misionesCompletadas = misionesCompletadas;
  }
}
