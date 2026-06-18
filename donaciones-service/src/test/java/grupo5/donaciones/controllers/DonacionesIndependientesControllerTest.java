package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.services.IDonacionesIndependientesService;
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
class DonacionesIndependientesControllerTest {

  private MockMvc mockMvc;

  @Mock private IDonacionesIndependientesService service;

  @InjectMocks private DonacionesIndependientesController controller;

  private ObjectMapper objectMapper;

  private static final String ACTOR = "SISTEMA";

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void cambiarEstado_DeberiaRetornarOk_CuandoTransicionEsExitosa() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, null);
    DonacionIndependienteResponseDTO response =
        new DonacionIndependienteResponseDTO(
            id, "AsignacionRealizada", List.of("AsignacionRealizada"));

    when(service.cambiarEstado(eq(id), any(), eq(ACTOR))).thenReturn(response);

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.estadoActual").value("AsignacionRealizada"))
        .andExpect(jsonPath("$.historialEstados[0]").value("AsignacionRealizada"));
  }

  @Test
  void cambiarEstado_DeberiaRetornarBadRequest_CuandoFaltaHeaderActor() throws Exception {
    MockMvc mockMvcWithoutAdvice = MockMvcBuilders.standaloneSetup(controller).build();
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, null);

    mockMvcWithoutAdvice
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cambiarEstado_DeberiaRetornarNotFound_CuandoRecursoNoExiste() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, null);

    when(service.cambiarEstado(eq(id), any(), eq(ACTOR)))
        .thenThrow(new RecursoNoEncontradoException(id));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(id.toString()));
  }

  @Test
  void cambiarEstado_DeberiaRetornarConflict_CuandoTransicionEsInvalida() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.ENTREGADA, null, null);

    when(service.cambiarEstado(eq(id), any(), eq(ACTOR)))
        .thenThrow(new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.code").value(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA.getCode()));
  }

  @Test
  void cambiarEstado_DeberiaRetornarBadRequest_CuandoArgumentoEsInvalido() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, "", null);

    when(service.cambiarEstado(eq(id), any(), eq(ACTOR)))
        .thenThrow(
            new IllegalArgumentException(
                "La justificación es obligatoria para registrar una entrega fallida."));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCatalog.ARGUMENTO_INVALIDO.getCode()));
  }
}
