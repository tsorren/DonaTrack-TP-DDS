package grupo5.logistica.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.logistica.controllers.impl.PlanificacionController;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PlanificacionControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private IPlanificacionService planificacionService;
  @InjectMocks private PlanificacionController controller;

  private static final UUID SOLICITUD_ID = UUID.randomUUID();
  private static final SolicitudPlanificacionResponseDTO RESPONSE_DTO =
      new SolicitudPlanificacionResponseDTO(
          SOLICITUD_ID,
          LocalDate.now(),
          EstadoSolicitud.PROCESADA,
          1,
          "http://callback",
          List.of(),
          0,
          null);

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  // ===================== POST /api/logistica/resultados =====================

  @Test
  void procesarCallback_deberiaRetornar200_cuandoCallbackEsOK() throws Exception {
    CallbackPlanificacionRequestDTO request =
        new CallbackPlanificacionRequestDTO(SOLICITUD_ID, List.of(), "OK", null);
    when(planificacionService.procesarCallback(any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            post("/api/logistica/resultados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(SOLICITUD_ID.toString()))
        .andExpect(jsonPath("$.estado").value("PROCESADA"));
  }

  @Test
  void procesarCallback_deberiaRetornar400_cuandoCallbackEsInvalido() throws Exception {
    CallbackPlanificacionRequestDTO request =
        new CallbackPlanificacionRequestDTO(null, null, null, null);

    mockMvc
        .perform(
            post("/api/logistica/resultados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void procesarCallback_deberiaRetornar404_cuandoSolicitudNoExiste() throws Exception {
    CallbackPlanificacionRequestDTO request =
        new CallbackPlanificacionRequestDTO(SOLICITUD_ID, List.of(), "OK", null);
    when(planificacionService.procesarCallback(any()))
        .thenThrow(new RecursoNoEncontradoException(SOLICITUD_ID));

    mockMvc
        .perform(
            post("/api/logistica/resultados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(SOLICITUD_ID.toString()));
  }

  // ===================== GET /api/logistica/planificaciones/{id} =====================

  @Test
  void obtenerPorId_deberiaRetornar200_cuandoSolicitudExiste() throws Exception {
    when(planificacionService.obtenerPorId(SOLICITUD_ID)).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(get("/api/logistica/planificaciones/" + SOLICITUD_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(SOLICITUD_ID.toString()))
        .andExpect(jsonPath("$.estado").value("PROCESADA"));
  }

  @Test
  void obtenerPorId_deberiaRetornar404_cuandoSolicitudNoExiste() throws Exception {
    when(planificacionService.obtenerPorId(SOLICITUD_ID))
        .thenThrow(new RecursoNoEncontradoException(SOLICITUD_ID));

    mockMvc
        .perform(get("/api/logistica/planificaciones/" + SOLICITUD_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(SOLICITUD_ID.toString()));
  }
}
