package grupo5.logistica.controllers;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class PlanificacionManualControllerTest extends AbstractLogisticaWebMvcTest {

  @Test
  void iniciarPlanificacion_deberiaDelegarYRetornar202() throws Exception {
    mockMvc
        .perform(post("/api/logistica/planificaciones/ejecuciones"))
        .andExpect(status().isAccepted());

    verify(planificacionService).iniciarPlanificacion();
  }
}
