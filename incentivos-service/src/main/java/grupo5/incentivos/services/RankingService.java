package grupo5.incentivos.services;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.RankingMensualRepository;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

  private final DonanteIncentivosRepository donanteRepository;
  private final RankingMensualRepository rankingRepository;
  private final N8nClient n8nClient;

  public RankingService(
      DonanteIncentivosRepository donanteRepository,
      RankingMensualRepository rankingRepository,
      N8nClient n8nClient) {
    this.donanteRepository = donanteRepository;
    this.rankingRepository = rankingRepository;
    this.n8nClient = n8nClient;
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
                    d.getNombre(),
                    d.misionesCompletadasEnMes(periodo.getYear(), periodo.getMonthValue())))
        .filter(e -> e.getMisionesCompletadas() > 0)
        .sorted(Comparator.comparingLong(EntradaRanking::getMisionesCompletadas).reversed())
        .forEach(
            entrada -> {
              entrada.setPosicion(posicion.getAndIncrement());
              ranking.agregarEntrada(entrada);
            });

    rankingRepository.guardar(ranking);
    RankingMensualDTO resultado = RankingMensualDTO.desde(ranking);

    // Notificar a n8n con el top 3 para que "publique" en red social (mock)
    List<Map<String, Object>> top3 =
        resultado.entradas().stream()
            .filter(e -> e.posicion() <= 3)
            .map(
                e ->
                    Map.<String, Object>of(
                        "posicion", e.posicion(),
                        "donanteId", e.donanteId(),
                        "nombre", e.nombreDonante(),
                        "misionesCompletadas", e.misionesCompletadas()))
            .toList();

    n8nClient.notificarRankingCalculado(periodo.toString(), top3);

    return resultado;
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
