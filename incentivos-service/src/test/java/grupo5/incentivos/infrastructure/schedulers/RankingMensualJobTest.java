package grupo5.incentivos.infrastructure.schedulers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import grupo5.incentivos.services.IRankingService;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingMensualJobTest {

  @Mock private IRankingService rankingService;

  @InjectMocks private RankingMensualJob job;

  @Test
  void ejecutarRankingMensual_deberiaInvocarCalcularYNotificarConPeriodoActual() {
    job.ejecutarRankingMensual();

    ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
    verify(rankingService).calcularYNotificar(captor.capture());

    YearMonth esperado = YearMonth.now(ZoneId.systemDefault());
    assertEquals(esperado, captor.getValue());
  }

  @Test
  void ejecutarRankingMensual_cuandoFallaElServicio_noDeberiaPropagarLaExcepcion() {
    doThrow(new RuntimeException("Fallo en servicio de ranking o n8n"))
        .when(rankingService)
        .calcularYNotificar(any());

    assertDoesNotThrow(() -> job.ejecutarRankingMensual());

    verify(rankingService).calcularYNotificar(any());
  }
}
