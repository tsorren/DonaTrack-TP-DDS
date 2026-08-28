package grupo5.incentivos.models.entities.ranking;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class RankingMensual implements AggregateRoot {
  private final UUID id;
  private YearMonth periodo;
  private List<EntradaRanking> entradas;

  public RankingMensual(YearMonth periodo) {
    if (periodo == null) {
      throw new ValidationException(ErrorCatalog.RANKING_PERIODO_NULO);
    }
    this.id = UUID.randomUUID();
    this.periodo = periodo;
    this.entradas = new ArrayList<>();
  }

  public void agregarEntrada(EntradaRanking entrada) {
    if (entrada == null) {
      throw new ValidationException(ErrorCatalog.RANKING_ENTRADA_NULA);
    }
    this.entradas.add(entrada);
  }

  public List<EntradaRanking> getPodio() {
    return this.entradas.stream().limit(3).toList();
  }
}
