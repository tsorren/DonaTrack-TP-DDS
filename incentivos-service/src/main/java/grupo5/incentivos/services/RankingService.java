package grupo5.incentivos.services;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.infrastructure.IN8nClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.GestorDeRankings;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.IRankingRepository;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RankingService implements IRankingService {

  private final IDonanteIncentivosRepository donanteRepository;
  private final IRankingRepository rankingRepository;
  private final IN8nClient n8nClient;
  private final GestorDeRankings gestorDeRankings;

  public RankingService(
      IDonanteIncentivosRepository donanteRepository,
      IRankingRepository rankingRepository,
      IN8nClient n8nClient,
      GestorDeRankings gestorDeRankings) {
    this.donanteRepository = donanteRepository;
    this.rankingRepository = rankingRepository;
    this.n8nClient = n8nClient;
    this.gestorDeRankings = gestorDeRankings;
  }

  @Override
  public RankingMensualDTO calcularYPersistir(YearMonth periodo) {
    rankingRepository.findByPeriodo(periodo).ifPresent(rankingRepository::delete);
    List<DonanteIncentivos> todos = donanteRepository.findAll();

<<<<<<< HEAD
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
=======
    RankingMensual ranking = gestorDeRankings.calcular(todos, periodo);
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb

    // FIN LOGICA DE NEGOCIO

    rankingRepository.save(ranking);
    return RankingMensualDTO.desde(ranking);
  }

  @Override
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

  @Override
  public Optional<Integer> obtenerPosicionDonante(UUID donanteId) {
    return obtenerUltimoRanking()
        .flatMap(
            r ->
                r.entradas().stream()
                    .filter(e -> e.donanteId().equals(donanteId))
                    .findFirst()
                    .map(RankingMensualDTO.EntradaRankingDTO::posicion));
  }

  @Override
  public Optional<Integer> obtenerPosicionDonante(UUID donanteId, YearMonth periodo) {
    return obtenerRankingPorPeriodo(periodo)
        .flatMap(
            r ->
                r.getEntradas().stream()
                    .filter(e -> e.getDonanteId().equals(donanteId))
                    .findFirst()
                    .map(EntradaRanking::getPosicion));
  }

  @Override
  public Optional<RankingMensual> obtenerRankingPorPeriodo(YearMonth periodo) {
    return rankingRepository.findByPeriodo(periodo);
  }

  @Override
  public List<RankingMensualDTO> obtenerHistorial() {
    return rankingRepository.findAll().stream().map(RankingMensualDTO::desde).toList();
  }

  @Override
  public Optional<RankingMensualDTO> obtenerUltimoRanking() {
    return rankingRepository.findAll().stream()
        .max(Comparator.comparing(RankingMensual::getPeriodo))
        .map(RankingMensualDTO::desde);
  }
}
