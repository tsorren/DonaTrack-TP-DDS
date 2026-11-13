package grupo5.incentivos.services;

import grupo5.incentivos.dto.HistorialDeRankings;
import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

public interface IRankingService {
  RankingMensualDTO calcularYPersistir(YearMonth periodo);

  RankingMensualDTO calcularYNotificar(YearMonth periodo);

  Optional<Integer> obtenerPosicionDonante(UUID donanteId);

  Optional<Integer> obtenerPosicionDonante(UUID donanteId, YearMonth periodo);

  Optional<RankingMensual> obtenerRankingPorPeriodo(YearMonth periodo);

  /**
   * Una página del historial (R11-43), del mes más reciente al más viejo, con el total de meses.
   * {@code pagina} desde 0 y {@code tamanio} de 1 a 100; fuera de rango, {@code ERR-CSR-003}.
   */
  HistorialDeRankings obtenerHistorial(int pagina, int tamanio);

  Optional<RankingMensualDTO> obtenerUltimoRanking();
}
