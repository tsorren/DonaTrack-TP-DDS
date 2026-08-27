package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.infrastructure.IN8nClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.ranking.GestorDeRankings;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.RankingRepository;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

  private RankingService service;
  private DonanteIncentivosRepository donanteRepository;
  private RankingRepository rankingRepository;
  private GestorDeRankings gestorDeRankings;

  @Mock private IN8nClient n8nClient;

  @BeforeEach
  void setUp() {
    donanteRepository = new DonanteIncentivosRepository();
    rankingRepository = new RankingRepository();
    gestorDeRankings = new GestorDeRankings();
    service = new RankingService(donanteRepository, rankingRepository, n8nClient, gestorDeRankings);
  }

  @Test
  void calcularYPersistir_deberiaOrdenarPorMisionesCompletadasDescendente() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();

    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(id1, "Donante 1", mayo, 2);
    DonanteIncentivos d2 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(id2, "Donante 2", mayo, 5);
    DonanteIncentivos d3 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(id3, "Donante 3", mayo, 1);

    donanteRepository.save(d1);
    donanteRepository.save(d2);
    donanteRepository.save(d3);

    RankingMensualDTO ranking = service.calcularYPersistir(mayo);

    assertEquals(3, ranking.entradas().size());
    assertEquals(id2, ranking.entradas().get(0).donanteId());
    assertEquals(1, ranking.entradas().get(0).posicion());
    assertEquals(id1, ranking.entradas().get(1).donanteId());
    assertEquals(2, ranking.entradas().get(1).posicion());
    assertEquals(id3, ranking.entradas().get(2).donanteId());
    assertEquals(3, ranking.entradas().get(2).posicion());
  }

  @Test
  void calcularYPersistir_deberiaExcluirDonantesConCeroMisionesEnElMes() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(id1, "Donante 1", mayo, 3);
    DonanteIncentivos d2 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(id2, "Donante 2", mayo, 0);

    donanteRepository.save(d1);
    donanteRepository.save(d2);

    RankingMensualDTO ranking = service.calcularYPersistir(mayo);

    assertEquals(1, ranking.entradas().size());
    assertEquals(id1, ranking.entradas().get(0).donanteId());
  }

  @Test
  void calcularYPersistir_deberiaRetornarVacioSiNadieTieneMisiones() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(
            UUID.randomUUID(), "Donante 1", mayo, 0);
    donanteRepository.save(d1);

    RankingMensualDTO ranking = service.calcularYPersistir(mayo);

    assertTrue(ranking.entradas().isEmpty());
    assertTrue(ranking.podio().isEmpty());
  }

  @Test
  void calcularYPersistir_podioDeberiaLimitarseATresDonantes() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    for (int i = 1; i <= 5; i++) {
      donanteRepository.save(
          DonanteIncentivosMother.conMisionesCompletadasEnMes(
              UUID.randomUUID(), "Donante " + i, mayo, i));
    }

    RankingMensualDTO ranking = service.calcularYPersistir(mayo);

    assertEquals(5, ranking.entradas().size());
    assertEquals(3, ranking.podio().size());
  }

  @Test
  void calcularYNotificar_deberiaNotificarAn8n() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(
            UUID.randomUUID(), "Donante 1", mayo, 3);
    donanteRepository.save(d1);

    service.calcularYNotificar(mayo);

    verify(n8nClient, times(1)).notificarRankingCalculado(eq("2026-05"), any());
  }

  @Test
  void obtenerPosicionDonante_porPeriodo_cuandoDonanteParticipo_deberiaRetornarPosicion() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    UUID id = UUID.randomUUID();
    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(id, "Donante 1", mayo, 3);
    donanteRepository.save(d1);
    service.calcularYPersistir(mayo);

    Optional<Integer> posicion = service.obtenerPosicionDonante(id, mayo);

    assertTrue(posicion.isPresent());
    assertEquals(1, posicion.get());
  }

  @Test
  void obtenerPosicionDonante_porPeriodo_cuandoNoHayRanking_deberiaRetornarVacio() {
    Optional<Integer> posicion =
        service.obtenerPosicionDonante(UUID.randomUUID(), YearMonth.of(2026, Month.MAY));

    assertTrue(posicion.isEmpty());
  }

  @Test
  void obtenerPosicionDonante_porPeriodo_cuandoDonanteNoParticipo_deberiaRetornarVacio() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(
            UUID.randomUUID(), "Donante 1", mayo, 3);
    donanteRepository.save(d1);
    service.calcularYPersistir(mayo);

    Optional<Integer> posicion = service.obtenerPosicionDonante(UUID.randomUUID(), mayo);

    assertTrue(posicion.isEmpty());
  }

  @Test
  void obtenerPosicionDonante_ultimoRanking_cuandoDonanteParticipo_deberiaRetornarPosicion() {
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    UUID id = UUID.randomUUID();
    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(id, "Donante 1", mayo, 3);
    donanteRepository.save(d1);
    service.calcularYPersistir(mayo);

    Optional<Integer> posicion = service.obtenerPosicionDonante(id);

    assertTrue(posicion.isPresent());
    assertEquals(1, posicion.get());
  }

  @Test
  void obtenerPosicionDonante_ultimoRanking_cuandoNoHayRanking_deberiaRetornarVacio() {
    Optional<Integer> posicion = service.obtenerPosicionDonante(UUID.randomUUID());

    assertTrue(posicion.isEmpty());
  }

  @Test
  void obtenerUltimoRanking_deberiaRetornarElMasReciente() {
    YearMonth abril = YearMonth.of(2026, Month.APRIL);
    YearMonth mayo = YearMonth.of(2026, Month.MAY);

    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(UUID.randomUUID(), "D1", abril, 2);
    DonanteIncentivos d2 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(UUID.randomUUID(), "D2", mayo, 4);
    donanteRepository.save(d1);
    donanteRepository.save(d2);

    service.calcularYPersistir(abril);
    service.calcularYPersistir(mayo);

    Optional<RankingMensualDTO> ultimo = service.obtenerUltimoRanking();

    assertTrue(ultimo.isPresent());
    assertEquals("2026-05", ultimo.get().periodo());
  }

  @Test
  void obtenerUltimoRanking_cuandoNoHayRankings_deberiaRetornarVacio() {
    assertTrue(service.obtenerUltimoRanking().isEmpty());
  }

  @Test
  void obtenerHistorial_deberiaRetornarTodosLosRankings() {
    YearMonth abril = YearMonth.of(2026, Month.APRIL);
    YearMonth mayo = YearMonth.of(2026, Month.MAY);

    DonanteIncentivos d1 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(UUID.randomUUID(), "D1", abril, 2);
    DonanteIncentivos d2 =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(UUID.randomUUID(), "D2", mayo, 4);
    donanteRepository.save(d1);
    donanteRepository.save(d2);

    service.calcularYPersistir(abril);
    service.calcularYPersistir(mayo);

    List<RankingMensualDTO> historial = service.obtenerHistorial();

    assertEquals(2, historial.size());
  }
}
