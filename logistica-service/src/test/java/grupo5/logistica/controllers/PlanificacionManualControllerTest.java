package grupo5.logistica.controllers;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.logistica.controllers.impl.PlanificacionManualController;
import grupo5.logistica.services.IPlanificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = PlanificacionManualController.class,
    properties = "logistica.planificacion.manual-enabled=true")
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
class PlanificacionManualControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IPlanificacionService planificacionService;

  @Test
  void iniciarPlanificacion_deberiaDelegarYRetornar202() throws Exception {
    mockMvc
        .perform(post("/api/logistica/planificaciones/ejecuciones"))
        .andExpect(status().isAccepted());

    verify(planificacionService).iniciarPlanificacion();
  }
}
