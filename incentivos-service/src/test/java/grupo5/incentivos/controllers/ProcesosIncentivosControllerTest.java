package grupo5.incentivos.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.incentivos.services.IInactividadService;
import grupo5.incentivos.services.IMisionesDonacionService;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ProcesosIncentivosControllerTest {

  private ProcesosIncentivosController controller;

  @Mock private IInactividadService inactividadService;
  @Mock private IMisionesDonacionService misionesDonacionService;

  @BeforeEach
  void setUp() {
    controller = new ProcesosIncentivosController(inactividadService, misionesDonacionService);
  }

  @Test
  void ejecutarEvaluacionInactividad_deberiaRetornar200OkYDelegarAlServicio() {
    ResponseEntity<Void> response = controller.ejecutarEvaluacionInactividad();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(inactividadService, times(1)).procesarInactividad();
  }

  @Test
  void ejecutarVerificacionRachas_deberiaUsarSiempreElMesActual() {
    ResponseEntity<Void> response = controller.ejecutarVerificacionRachas();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(misionesDonacionService, times(1)).verificarRachasVencidas(YearMonth.now());
  }
}
