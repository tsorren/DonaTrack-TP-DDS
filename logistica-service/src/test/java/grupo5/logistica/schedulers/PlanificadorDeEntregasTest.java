package grupo5.logistica.schedulers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import grupo5.logistica.services.IPlanificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanificadorDeEntregasTest {

  @Mock private IPlanificacionService planificacionService;

  private PlanificadorDeEntregas planificador;

  @BeforeEach
  void setUp() {
    planificador = new PlanificadorDeEntregas(planificacionService);
  }

  @Test
  @DisplayName(
      "Al ejecutarse el ciclo automático, debe delegar el inicio de la planificación al service")
  void ejecutar_debeDelegarEnElService() {
    planificador.ejecutar();

    verify(planificacionService, times(1)).iniciarPlanificacion();
    verifyNoMoreInteractions(planificacionService);
  }
}
