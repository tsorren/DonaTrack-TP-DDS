package grupo5.incentivos.services;

import grupo5.incentivos.dto.RankingMensualDTO;
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
import org.springframework.stereotype.Service;

@Service
public class RankingService {

  private final DonanteIncentivosRepository donanteRepository;
  private final RankingMensualRepository rankingRepository;

  public RankingService(
      DonanteIncentivosRepository donanteRepository, RankingMensualRepository rankingRepository) {
    this.donanteRepository = donanteRepository;
    this.rankingRepository = rankingRepository;
  }

  public RankingMensualDTO calcularYPersistir(YearMonth periodo) {
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
    return RankingMensualDTO.desde(ranking);
  }

  public Optional<Integer> obtenerPosicionDonante(Long donanteId) {
    return obtenerUltimoRanking()
        .flatMap(
            r ->
                r.entradas().stream()
                    .filter(e -> e.donanteId().equals(donanteId))
                    .findFirst()
                    .map(RankingMensualDTO.EntradaRankingDTO::posicion));
  }

  public Optional<RankingMensual> obtenerRankingPorPeriodo(YearMonth periodo) {
    return rankingRepository.buscarPorPeriodo(periodo);
  }

  public List<RankingMensualDTO> obtenerHistorial() {
    return rankingRepository.listarTodos().stream().map(RankingMensualDTO::desde).toList();
  }

  public Optional<RankingMensualDTO> obtenerUltimoRanking() {
    return rankingRepository.listarTodos().stream()
        .max(Comparator.comparing(RankingMensual::getPeriodo))
        .map(RankingMensualDTO::desde);
  }
}
