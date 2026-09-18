package grupo5.incentivos.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.fixtures.RankingMensualMother;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.services.IRankingService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RankingControllerTest {

  private RankingController controller;

  @Mock private IRankingService rankingService;

  @BeforeEach
  void setUp() {
    controller = new RankingController(rankingService);
  }

  @Test
  void obtenerUltimoRanking_cuandoExiste_deberiaRetornarStatus200OkYBody() {
    RankingMensualDTO dto = RankingMensualDTO.desde(RankingMensualMother.vacioDeMayo2026());

    when(rankingService.obtenerUltimoRanking()).thenReturn(Optional.of(dto));

    ResponseEntity<RankingMensualDTO> response = controller.obtenerUltimoRanking();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(dto, response.getBody());
    verify(rankingService, times(1)).obtenerUltimoRanking();
  }

  @Test
  void obtenerUltimoRanking_cuandoNoExiste_deberiaRetornarStatus204NoContent() {
    when(rankingService.obtenerUltimoRanking()).thenReturn(Optional.empty());

    ResponseEntity<RankingMensualDTO> response = controller.obtenerUltimoRanking();

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void obtenerHistorial_deberiaRetornarStatus200OkYLista() {
    List<RankingMensualDTO> historial =
        List.of(RankingMensualDTO.desde(RankingMensualMother.vacioDeMayo2026()));

    when(rankingService.obtenerHistorial()).thenReturn(historial);

    ResponseEntity<List<RankingMensualDTO>> response = controller.obtenerHistorial();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(historial, response.getBody());
  }

  @Test
  void calcularRanking_conPeriodoEspecificado_deberiaPasarElPeriodo() {
    RankingMensualDTO dto = RankingMensualDTO.desde(RankingMensualMother.vacioDeMayo2026());

    when(rankingService.calcularYPersistir(YearMonth.of(2026, Month.MAY))).thenReturn(dto);

    ResponseEntity<RankingMensualDTO> response = controller.calcularRanking("2026-05");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(rankingService, times(1)).calcularYPersistir(YearMonth.of(2026, Month.MAY));
  }

  @Test
  void calcularRanking_sinPeriodo_deberiaUsarMesActual() {
    RankingMensualDTO dto = RankingMensualDTO.desde(RankingMensualMother.vacioDeMayo2026());

    when(rankingService.calcularYPersistir(any(YearMonth.class))).thenReturn(dto);

    ResponseEntity<RankingMensualDTO> response = controller.calcularRanking(null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(rankingService, times(1)).calcularYPersistir(any(YearMonth.class));
  }

  @Test
  void obtenerPosicionDonante_conPeriodo_cuandoExiste_deberiaRetornar200OkYPosicion() {
    UUID donanteId = UUID.randomUUID();
    when(rankingService.obtenerPosicionDonante(donanteId, YearMonth.of(2026, Month.MAY)))
        .thenReturn(Optional.of(3));

    ResponseEntity<Integer> response = controller.obtenerPosicionDonante(donanteId, "2026-05");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(3, response.getBody());
    verify(rankingService, times(1))
        .obtenerPosicionDonante(donanteId, YearMonth.of(2026, Month.MAY));
  }

  @Test
  void obtenerPosicionDonante_sinPeriodo_cuandoExiste_deberiaRetornar200OkYPosicion() {
    UUID donanteId = UUID.randomUUID();
    when(rankingService.obtenerPosicionDonante(donanteId)).thenReturn(Optional.of(1));

    ResponseEntity<Integer> response = controller.obtenerPosicionDonante(donanteId, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody());
    verify(rankingService, times(1)).obtenerPosicionDonante(donanteId);
  }

  @Test
  void obtenerPosicionDonante_cuandoNoExiste_deberiaRetornar204NoContent() {
    UUID donanteId = UUID.randomUUID();
    when(rankingService.obtenerPosicionDonante(donanteId)).thenReturn(Optional.empty());

    ResponseEntity<Integer> response = controller.obtenerPosicionDonante(donanteId, null);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void obtenerRankingPorPeriodo_cuandoExiste_deberiaRetornarStatus200OkYBody() {
    RankingMensual ranking = RankingMensualMother.vacioDeMayo2026();

    when(rankingService.obtenerRankingPorPeriodo(YearMonth.of(2026, Month.MAY)))
        .thenReturn(Optional.of(ranking));

    ResponseEntity<RankingMensualDTO> response = controller.obtenerRankingPorPeriodo("2026-05");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(RankingMensualDTO.desde(ranking), response.getBody());
    verify(rankingService, times(1)).obtenerRankingPorPeriodo(YearMonth.of(2026, Month.MAY));
  }

  @Test
  void obtenerRankingPorPeriodo_cuandoNoExiste_deberiaLanzarBusinessStateException() {
    when(rankingService.obtenerRankingPorPeriodo(YearMonth.of(2026, Month.MAY)))
        .thenReturn(Optional.empty());

    assertThrows(
        BusinessStateException.class, () -> controller.obtenerRankingPorPeriodo("2026-05"));
  }
}
