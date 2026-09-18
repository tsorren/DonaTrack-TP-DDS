package grupo5.incentivos.services;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRankingService {
  RankingMensualDTO calcularYPersistir(YearMonth periodo);

  RankingMensualDTO calcularYNotificar(YearMonth periodo);

  Optional<Integer> obtenerPosicionDonante(UUID donanteId);

  Optional<Integer> obtenerPosicionDonante(UUID donanteId, YearMonth periodo);

  Optional<RankingMensual> obtenerRankingPorPeriodo(YearMonth periodo);

  List<RankingMensualDTO> obtenerHistorial();

  Optional<RankingMensualDTO> obtenerUltimoRanking();
}
