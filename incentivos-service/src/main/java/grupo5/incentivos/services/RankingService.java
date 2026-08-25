package grupo5.incentivos.services;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.IRankingRepository;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class RankingService implements IRankingService {

  private final IDonanteIncentivosRepository donanteRepository;
  private final IRankingRepository rankingRepository;
  private final N8nClient n8nClient;

  public RankingService(
      IDonanteIncentivosRepository donanteRepository,
      IRankingRepository rankingRepository,
      N8nClient n8nClient) {
    this.donanteRepository = donanteRepository;
    this.rankingRepository = rankingRepository;
    this.n8nClient = n8nClient;
  }

  public RankingMensualDTO calcularYPersistir(YearMonth periodo) {
    rankingRepository.findByPeriodo(periodo).ifPresent(rankingRepository::delete);
    List<DonanteIncentivos> todos = donanteRepository.findAll();
    RankingMensual ranking = new RankingMensual(periodo);
    AtomicInteger posicion = new AtomicInteger(1);

    // INICIO LOGICA DE NEGOCIO

    todos.stream()
        .map(
            d ->
                new EntradaRanking(
                    0,
                    d.getId(),
                    d.getNombre(),
                    d.misionesCompletadasEnMes(periodo.getYear(), periodo.getMonthValue())))
        .filter(e -> e.getMisionesCompletadas() > 0)
        .sorted(Comparator.comparingLong(EntradaRanking::getMisionesCompletadas).reversed())
        .forEach(
            entrada -> {
              entrada.setPosicion(posicion.getAndIncrement());
              ranking.agregarEntrada(entrada);
            });

    // FIN LOGICA DE NEGOCIO

    rankingRepository.save(ranking);
    return RankingMensualDTO.desde(ranking);
    // ← sin n8n
  }

  public RankingMensualDTO calcularYNotificar(YearMonth periodo) {
    RankingMensualDTO resultado = calcularYPersistir(periodo);

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

  public Optional<Integer> obtenerPosicionDonante(UUID donanteId) {
    return obtenerUltimoRanking()
        .flatMap(
            r ->
                r.entradas().stream()
                    .filter(e -> e.donanteId().equals(donanteId))
                    .findFirst()
                    .map(RankingMensualDTO.EntradaRankingDTO::posicion));
  }

  public Optional<RankingMensual> obtenerRankingPorPeriodo(YearMonth periodo) {
    return rankingRepository.findByPeriodo(periodo);
  }

  public List<RankingMensualDTO> obtenerHistorial() {
    return rankingRepository.findAll().stream().map(RankingMensualDTO::desde).toList();
  }

  public Optional<RankingMensualDTO> obtenerUltimoRanking() {
    return rankingRepository.findAll().stream()
        .max(Comparator.comparing(RankingMensual::getPeriodo))
        .map(RankingMensualDTO::desde);
  }
}
