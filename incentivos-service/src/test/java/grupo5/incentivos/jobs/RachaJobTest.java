package grupo5.incentivos.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import grupo5.incentivos.services.IMisionesDonacionService;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RachaJobTest {

  @Mock private IMisionesDonacionService misionesDonacionService;

  @InjectMocks private RachaJob job;

  @Test
  void verificarRachasVencidas_deberiaInvocarServicioConMesActual() {
    job.verificarRachasVencidas();

    ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
    verify(misionesDonacionService).verificarRachasVencidas(captor.capture());

    YearMonth esperado = YearMonth.now(ZoneId.systemDefault());
    assertEquals(esperado, captor.getValue());
  }
}
