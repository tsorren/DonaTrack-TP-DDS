package grupo5.donaciones.schedulers;

import static org.mockito.Mockito.*;

import grupo5.donaciones.services.impl.PlanificacionNecesidadesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanificadorDeNecesidadesTest {

  private PlanificacionNecesidadesService planificacionServiceMock;
  private PlanificadorDeNecesidades planificador;

  @BeforeEach
  void setUp() {
    planificacionServiceMock = mock(PlanificacionNecesidadesService.class);
    planificador = new PlanificadorDeNecesidades(planificacionServiceMock);
  }

  @Test
  void ejecutarPlanificacion_deberiaDelegar_AlServicio_ExactamenteUnaVez() {
    planificador.ejecutarPlanificacionDeNecesidades();

    verify(planificacionServiceMock, times(1)).generarNuevosPeriodosParaNecesidadesRecurrentes();
  }
}
