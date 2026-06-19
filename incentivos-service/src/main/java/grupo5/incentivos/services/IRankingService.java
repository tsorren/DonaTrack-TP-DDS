package grupo5.incentivos.services;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.*;

public interface IRankingService {
  RankingMensualDTO calcularYPersistir(YearMonth periodo);

  RankingMensualDTO calcularYNotificar(YearMonth periodo);

  Optional<Integer> obtenerPosicionDonante(UUID donanteId);

  Optional<RankingMensual> obtenerRankingPorPeriodo(YearMonth periodo);

  List<RankingMensualDTO> obtenerHistorial();

  Optional<RankingMensualDTO> obtenerUltimoRanking();
}
