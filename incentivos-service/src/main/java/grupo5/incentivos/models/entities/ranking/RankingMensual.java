package grupo5.incentivos.models.entities.ranking;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankingMensual {

  private YearMonth periodo;
  private List<EntradaRanking> entradas;

  public RankingMensual(YearMonth periodo) {
    if (periodo == null) {
      throw new IllegalArgumentException("El periodo del ranking no puede ser nulo");
    }
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
