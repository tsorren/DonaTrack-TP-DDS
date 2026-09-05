package grupo5.logistica.controllers;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.logistica.controllers.impl.PlanificacionManualController;
import grupo5.logistica.services.IPlanificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PlanificacionManualControllerTest {

  private MockMvc mockMvc;

  @Mock private IPlanificacionService planificacionService;
  @InjectMocks private PlanificacionManualController controller;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void iniciarPlanificacion_deberiaDelegarYRetornar202() throws Exception {
    mockMvc
        .perform(post("/api/logistica/planificaciones/ejecuciones"))
        .andExpect(status().isAccepted());

    verify(planificacionService).iniciarPlanificacion();
  }
}
