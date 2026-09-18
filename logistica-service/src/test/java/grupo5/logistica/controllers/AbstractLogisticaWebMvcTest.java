package grupo5.logistica.controllers;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.logistica.controllers.impl.CamionesController;
import grupo5.logistica.controllers.impl.ChoferesController;
import grupo5.logistica.controllers.impl.EntregasController;
import grupo5.logistica.controllers.impl.PlanificacionController;
import grupo5.logistica.controllers.impl.PlanificacionManualController;
import grupo5.logistica.controllers.impl.RutasController;
import grupo5.logistica.services.ICamionesService;
import grupo5.logistica.services.IChoferesService;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.IRutasService;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {
      CamionesController.class,
      ChoferesController.class,
      EntregasController.class,
      PlanificacionController.class,
      PlanificacionManualController.class,
      RutasController.class
    },
    properties = "logistica.planificacion.manual-enabled=true")
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("logistica-webmvc-context")
public abstract class AbstractLogisticaWebMvcTest {

  @Autowired protected MockMvc mockMvc;

  @MockitoBean protected ICamionesService camionesService;

  @MockitoBean protected IChoferesService choferesService;

  @MockitoBean protected IEntregasService entregasService;

  @MockitoBean protected IPlanificacionService planificacionService;

  @MockitoBean protected IRutasService rutasService;
}
