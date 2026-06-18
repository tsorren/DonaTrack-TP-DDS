package grupo5.incentivos.models.entities.ranking;

import grupo5.common.repositories.AggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RankingMensual implements AggregateRoot {
  private final UUID id;
  private YearMonth periodo;
  private List<EntradaRanking> entradas;

  public RankingMensual(YearMonth periodo) {
    if (periodo == null) {
      throw new IllegalArgumentException("El periodo del ranking no puede ser nulo");
    }
    this.id = UUID.randomUUID();
    this.periodo = periodo;
    this.entradas = new ArrayList<>();
  }

  public void agregarEntrada(EntradaRanking entrada) {
    if (entrada == null) {
      throw new IllegalArgumentException("La entrada no puede ser nula");
    }
    this.entradas.add(entrada);
  }

  public List<EntradaRanking> getPodio() {
    return this.entradas.stream().limit(3).toList();
  }
}
