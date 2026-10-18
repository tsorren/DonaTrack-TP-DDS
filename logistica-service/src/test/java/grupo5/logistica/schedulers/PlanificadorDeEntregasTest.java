package grupo5.logistica.schedulers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import grupo5.logistica.services.IPlanificacionService;
import org.junit.jupiter.api.Test;

class PlanificadorDeEntregasTest {

  @Test
  void ejecutarSoloActivaElCasoDeUso() {
    IPlanificacionService planificacionService = mock(IPlanificacionService.class);

    new PlanificadorDeEntregas(planificacionService).ejecutar();

    verify(planificacionService).iniciarPlanificacion();
  }
}
