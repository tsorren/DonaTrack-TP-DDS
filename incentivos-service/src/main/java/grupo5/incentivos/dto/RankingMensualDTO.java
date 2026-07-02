package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.util.List;
import java.util.UUID;

public record RankingMensualDTO(
    String periodo, List<EntradaRankingDTO> entradas, List<EntradaRankingDTO> podio) {

  public RankingMensualDTO {
    entradas = entradas != null ? List.copyOf(entradas) : List.of();
    podio = podio != null ? List.copyOf(podio) : List.of();
  }

  public static RankingMensualDTO desde(RankingMensual ranking) {
    return new RankingMensualDTO(
        ranking.getPeriodo().toString(),
        ranking.getEntradas().stream().map(EntradaRankingDTO::desde).toList(),
        ranking.getPodio().stream().map(EntradaRankingDTO::desde).toList());
  }

  public record EntradaRankingDTO(
      int posicion, UUID donanteId, String nombreDonante, long misionesCompletadas) {

    public static EntradaRankingDTO desde(EntradaRanking entrada) {
      return new EntradaRankingDTO(
          entrada.getPosicion(),
          entrada.getDonanteId(),
          entrada.getNombreDonante(),
          entrada.getMisionesCompletadas());
    }
  }
}
