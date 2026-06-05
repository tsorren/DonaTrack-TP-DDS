package grupo5.incentivos.services;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.RankingMensualRepository;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

  private static final Logger log = LoggerFactory.getLogger(RankingService.class);

  private final DonanteIncentivosRepository donanteRepository;
  private final RankingMensualRepository rankingRepository;

  public RankingService(
      DonanteIncentivosRepository donanteRepository, RankingMensualRepository rankingRepository) {
    this.donanteRepository = donanteRepository;
    this.rankingRepository = rankingRepository;
  }

  public RankingMensual calcularYPersistir(YearMonth periodo) {
    log.info("Calculando ranking mensual para el periodo {}", periodo);

    List<DonanteIncentivos> todos = donanteRepository.listarTodos();

    RankingMensual ranking = new RankingMensual(periodo);
    AtomicInteger posicion = new AtomicInteger(1);

    todos.stream()
        .map(
            d ->
                new EntradaRanking(
                    0,
                    d.getDonanteId(),
                    "Donante #" + d.getDonanteId(),
                    d.misionesCompletadasEnMes(periodo.getYear(), periodo.getMonthValue())))
        .filter(e -> e.getMisionesCompletadas() > 0)
        .sorted(Comparator.comparingLong(EntradaRanking::getMisionesCompletadas).reversed())
        .forEach(
            entrada -> {
              entrada.setPosicion(posicion.getAndIncrement());
              ranking.agregarEntrada(entrada);
            });

    rankingRepository.guardar(ranking);
    log.info(
        "Ranking del período {} calculado con {} entradas.", periodo, ranking.getEntradas().size());
    return ranking;
  }

  public Optional<RankingMensual> obtenerRankingPorPeriodo(YearMonth periodo) {
    return rankingRepository.buscarPorPeriodo(periodo);
  }

  public List<RankingMensual> obtenerHistorial() {
    return rankingRepository.listarTodos();
  }

  public Optional<RankingMensual> obtenerUltimoRanking() {
    return rankingRepository.listarTodos().stream()
        .max(Comparator.comparing(RankingMensual::getPeriodo));
  }
}
