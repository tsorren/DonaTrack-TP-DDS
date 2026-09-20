package grupo5.incentivos.models.entities.ranking;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EntradaRanking {

  private final int posicion;
  private final UUID donanteId;
  private final String nombreDonante;
  private final long misionesCompletadas;

  public EntradaRanking(
      int posicion, UUID donanteId, String nombreDonante, long misionesCompletadas) {
    this.posicion = posicion;
    this.donanteId = donanteId;
    this.nombreDonante = nombreDonante;
    this.misionesCompletadas = misionesCompletadas;
  }
}
