package grupo5.logistica.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.logistica.controllers.impl.PlanificacionController;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.services.IPlanificacionService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PlanificacionControllerTest {

  private MockMvc mockMvc;

  @Mock private IPlanificacionService planificacionService;
  @InjectMocks private PlanificacionController controller;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void iniciarPlanificacion_deberiaRetornar201_conLasSolicitudesCreadas() throws Exception {
    SolicitudPlanificacionResponseDTO solicitud =
        new SolicitudPlanificacionResponseDTO(
            UUID.randomUUID(),
            LocalDate.now(),
            EstadoSolicitud.PENDIENTE,
            10,
            "http://localhost:8083/api/logistica/callback/rutas",
            List.of(),
            0,
            null);
    when(planificacionService.iniciarPlanificacion()).thenReturn(List.of(solicitud));

    mockMvc
        .perform(post("/api/logistica/planificaciones"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(solicitud.id().toString()))
        .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));

    verify(planificacionService, times(1)).iniciarPlanificacion();
  }

  @Test
  void iniciarPlanificacion_deberiaRetornar201_conListaVacia_cuandoNoHayNadaParaPlanificar()
      throws Exception {
    when(planificacionService.iniciarPlanificacion()).thenReturn(List.of());

    mockMvc
        .perform(post("/api/logistica/planificaciones"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

    verify(planificacionService, times(1)).iniciarPlanificacion();
  }

  @Test
  void obtenerPorId_deberiaRetornar200_conLaSolicitud() throws Exception {
    UUID id = UUID.randomUUID();
    SolicitudPlanificacionResponseDTO solicitud =
        new SolicitudPlanificacionResponseDTO(
            id, LocalDate.now(), EstadoSolicitud.PROCESADA, 5, "http://cb", List.of(), 0, null);
    when(planificacionService.obtenerPorId(id)).thenReturn(solicitud);

    mockMvc
        .perform(get("/api/logistica/planificaciones/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }
}
