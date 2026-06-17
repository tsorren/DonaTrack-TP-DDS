package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.RankingMensualRepository;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

  @Mock private N8nClient n8nClient;

  private DonanteIncentivosRepository donanteRepository;
  private RankingMensualRepository rankingRepository;
  private RankingService rankingService;

  private static final YearMonth PERIODO = YearMonth.of(2026, Month.MAY);

  @BeforeEach
  void setUp() {
    donanteRepository = new DonanteIncentivosRepository();
    rankingRepository = new RankingMensualRepository();
    rankingService = new RankingService(donanteRepository, rankingRepository, n8nClient);
  }

  @Test
  void calcularYPersistir_deberiaOrdenarPorMisionesCompletadasDescendente() {
    DonanteIncentivos ana = donanteConMisionesCompletadasEnMes(1L, "Ana", PERIODO, 3);
    DonanteIncentivos bob = donanteConMisionesCompletadasEnMes(2L, "Bob", PERIODO, 1);
    DonanteIncentivos carlos = donanteConMisionesCompletadasEnMes(3L, "Carlos", PERIODO, 2);
    donanteRepository.guardar(ana);
    donanteRepository.guardar(bob);
    donanteRepository.guardar(carlos);

    RankingMensualDTO resultado = rankingService.calcularYPersistir(PERIODO);

    assertEquals(3, resultado.entradas().size());
    assertEquals("Ana", resultado.entradas().get(0).nombreDonante());
    assertEquals("Carlos", resultado.entradas().get(1).nombreDonante());
    assertEquals("Bob", resultado.entradas().get(2).nombreDonante());
  }

  @Test
  void calcularYPersistir_deberiaAsignarPosicionesCorrectamente() {
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(1L, "Ana", PERIODO, 2));
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(2L, "Bob", PERIODO, 1));

    RankingMensualDTO resultado = rankingService.calcularYPersistir(PERIODO);

    assertEquals(1, resultado.entradas().get(0).posicion());
    assertEquals(2, resultado.entradas().get(1).posicion());
  }

  @Test
  void calcularYPersistir_deberiaExcluirDonantesConCeroMisionesEnElMes() {
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(1L, "Ana", PERIODO, 2));

    DonanteIncentivos bob = new DonanteIncentivos(2L, "Bob");
    donanteRepository.guardar(bob);

    RankingMensualDTO resultado = rankingService.calcularYPersistir(PERIODO);

    assertEquals(1, resultado.entradas().size());
    assertEquals("Ana", resultado.entradas().get(0).nombreDonante());
  }

  @Test
  void calcularYPersistir_deberiaRetornarVacioSiNadieTuvoDonacionesEnElMes() {
    donanteRepository.guardar(new DonanteIncentivos(1L, "Ana"));
    donanteRepository.guardar(new DonanteIncentivos(2L, "Bob"));

    RankingMensualDTO resultado = rankingService.calcularYPersistir(PERIODO);

    assertTrue(resultado.entradas().isEmpty());
  }

  @Test
  void calcularYPersistir_deberiaPersistirElRankingEnElRepositorio() {
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(1L, "Ana", PERIODO, 1));

    rankingService.calcularYPersistir(PERIODO);

    assertTrue(rankingRepository.buscarPorPeriodo(PERIODO).isPresent());
  }

  @Test
  void calcularYPersistir_deberiaNotificarN8nConElTop3() {
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(1L, "Ana", PERIODO, 3));
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(2L, "Bob", PERIODO, 2));
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(3L, "Carlos", PERIODO, 1));
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(4L, "Diana", PERIODO, 1));

    rankingService.calcularYPersistir(PERIODO);

    verify(n8nClient).notificarRankingCalculado(anyString(), anyList());
  }

  @Test
  void calcularYPersistir_podioDeberiaLimitarseATresDonantes() {
    for (long i = 1; i <= 5; i++) {
      donanteRepository.guardar(
          donanteConMisionesCompletadasEnMes(i, "Donante" + i, PERIODO, (int) i));
    }

    RankingMensualDTO resultado = rankingService.calcularYPersistir(PERIODO);

    assertEquals(3, resultado.podio().size());
  }

  @Test
  void obtenerPosicionDonante_deberiaRetornarPosicionCorrecta() {
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(1L, "Ana", PERIODO, 3));
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(2L, "Bob", PERIODO, 1));
    rankingService.calcularYPersistir(PERIODO);

    Optional<Integer> posicion = rankingService.obtenerPosicionDonante(2L);

    assertTrue(posicion.isPresent());
    assertEquals(2, posicion.get());
  }

  @Test
  void obtenerPosicionDonante_deberiaRetornarVacioCuandoNoHayRanking() {
    Optional<Integer> posicion = rankingService.obtenerPosicionDonante(1L);

    assertFalse(posicion.isPresent());
  }

  @Test
  void obtenerPosicionDonante_deberiaRetornarVacioCuandoDonanteNoEstaEnElRanking() {
    donanteRepository.guardar(donanteConMisionesCompletadasEnMes(1L, "Ana", PERIODO, 1));
    rankingService.calcularYPersistir(PERIODO);

    Optional<Integer> posicion = rankingService.obtenerPosicionDonante(99L);

    assertFalse(posicion.isPresent());
  }

  @Test
  void obtenerUltimoRanking_deberiaRetornarElMasReciente() {
    YearMonth periodoViejo = YearMonth.of(2026, Month.MARCH);
    YearMonth periodoNuevo = YearMonth.of(2026, Month.MAY);

    rankingRepository.guardar(new RankingMensual(periodoViejo));
    rankingRepository.guardar(new RankingMensual(periodoNuevo));

    Optional<RankingMensualDTO> ultimo = rankingService.obtenerUltimoRanking();

    assertTrue(ultimo.isPresent());
    assertEquals(periodoNuevo.toString(), ultimo.get().periodo());
  }

  @Test
  void obtenerUltimoRanking_deberiaRetornarVacioCuandoNoHayRankings() {
    Optional<RankingMensualDTO> ultimo = rankingService.obtenerUltimoRanking();

    assertFalse(ultimo.isPresent());
  }

  @Test
  void obtenerHistorial_deberiaRetornarTodosLosRankings() {
    rankingRepository.guardar(new RankingMensual(YearMonth.of(2026, Month.MARCH)));
    rankingRepository.guardar(new RankingMensual(YearMonth.of(2026, Month.APRIL)));
    rankingRepository.guardar(new RankingMensual(YearMonth.of(2026, Month.MAY)));

    List<RankingMensualDTO> historial = rankingService.obtenerHistorial();

    assertEquals(3, historial.size());
  }

  @Test
  void obtenerHistorial_deberiaRetornarListaVaciaSiNoHayRankings() {
    List<RankingMensualDTO> historial = rankingService.obtenerHistorial();

    assertTrue(historial.isEmpty());
  }

  @Test
  void obtenerRankingPorPeriodo_deberiaRetornarElRankingDelPeriodoCorrecto() {
    rankingRepository.guardar(new RankingMensual(PERIODO));

    Optional<RankingMensual> resultado = rankingService.obtenerRankingPorPeriodo(PERIODO);

    assertTrue(resultado.isPresent());
    assertEquals(PERIODO, resultado.get().getPeriodo());
  }

  @Test
  void obtenerRankingPorPeriodo_deberiaRetornarVacioCuandoNoBuscadoPeriodo() {
    rankingRepository.guardar(new RankingMensual(PERIODO));

    Optional<RankingMensual> resultado =
        rankingService.obtenerRankingPorPeriodo(YearMonth.of(2025, Month.JANUARY));

    assertFalse(resultado.isPresent());
  }

  private DonanteIncentivos donanteConMisionesCompletadasEnMes(
      long id, String nombre, YearMonth periodo, int cantidadMisiones) {
    DonanteIncentivos donante = new DonanteIncentivos(id, nombre);
    for (int i = 0; i < cantidadMisiones; i++) {
      MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
      EventoDonacion evento =
          EventoDonacion.builder()
              .donacionId((long) i)
              .fecha(LocalDate.of(periodo.getYear(), periodo.getMonthValue(), 15))
              .cantidadBienes(1)
              .categorias(List.of("arroz"))
              .build();
      mision.evaluarProgreso(donante, evento);
      donante.getMisiones().add(mision);
    }
    return donante;
  }
}
