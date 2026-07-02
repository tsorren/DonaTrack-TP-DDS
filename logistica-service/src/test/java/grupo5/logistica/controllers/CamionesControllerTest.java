package grupo5.logistica.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.logistica.controllers.impl.CamionesController;
import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.services.ICamionesService;
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
class CamionesControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private ICamionesService camionesService;
  @InjectMocks private CamionesController controller;

  private static final UUID ID = UUID.randomUUID();
  private static final CamionResponseDTO RESPONSE_DTO =
      new CamionResponseDTO(ID, "AB123CD", 10f, 2f, 5000f, EstadoCamion.DISPONIBLE, null);

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  // ===================== POST /api/camiones =====================

  @Test
  void crear_deberiaRetornar201_cuandoDatosValidos() throws Exception {
    CamionRequestDTO request = new CamionRequestDTO("AB123CD", 10f, 2f, 5000f);
    when(camionesService.crear(any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            post("/api/camiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.patente").value("AB123CD"))
        .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
  }

  @Test
  void crear_deberiaRetornar400_cuandoPatenteConFormatoInvalido() throws Exception {
    CamionRequestDTO request = new CamionRequestDTO("INVALIDA", 10f, 2f, 5000f);
    when(camionesService.crear(any()))
        .thenThrow(new ValidationException(ErrorCatalog.CAMION_PATENTE_FORMATO_INVALIDO));

    mockMvc
        .perform(
            post("/api/camiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorCatalog.CAMION_PATENTE_FORMATO_INVALIDO.getCode()));
  }

  @Test
  void crear_deberiaRetornar409_cuandoPatenteDuplicada() throws Exception {
    CamionRequestDTO request = new CamionRequestDTO("AB123CD", 10f, 2f, 5000f);
    when(camionesService.crear(any()))
        .thenThrow(new BusinessStateException(ErrorCatalog.CAMION_PATENTE_DUPLICADA));

    mockMvc
        .perform(
            post("/api/camiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCatalog.CAMION_PATENTE_DUPLICADA.getCode()));
  }

  // ===================== GET /api/camiones =====================

  @Test
  void listar_deberiaRetornar200_conListaDeCamiones() throws Exception {
    when(camionesService.consultarTodos()).thenReturn(List.of(RESPONSE_DTO));

    mockMvc
        .perform(get("/api/camiones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].patente").value("AB123CD"));
  }

  @Test
  void listar_deberiaRetornar200_conListaVacia() throws Exception {
    when(camionesService.consultarTodos()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/camiones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ===================== GET /api/camiones/{id} =====================

  @Test
  void consultarPorId_deberiaRetornar200_cuandoCamionExiste() throws Exception {
    when(camionesService.consultarPorId(ID)).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(get("/api/camiones/" + ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @Test
  void consultarPorId_deberiaRetornar404_cuandoCamionNoExiste() throws Exception {
    when(camionesService.consultarPorId(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(get("/api/camiones/" + ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(ID.toString()));
  }

  // ===================== PATCH /api/camiones/{id}/estado =====================

  @Test
  void cambiarEstado_deberiaRetornar200_cuandoTransicionValida() throws Exception {
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.DESHABILITADO, null);
    CamionResponseDTO responseDeshabilitado =
        new CamionResponseDTO(ID, "AB123CD", 10f, 2f, 5000f, EstadoCamion.DESHABILITADO, null);

    when(camionesService.cambiarEstado(eq(ID), any())).thenReturn(responseDeshabilitado);

    mockMvc
        .perform(
            patch("/api/camiones/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("DESHABILITADO"));
  }

  @Test
  void cambiarEstado_deberiaRetornar400_cuandoSeIntentaPasarAEnRuta() throws Exception {
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.EN_RUTA, null);
    when(camionesService.cambiarEstado(eq(ID), any()))
        .thenThrow(new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA));

    mockMvc
        .perform(
            patch("/api/camiones/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cambiarEstado_deberiaRetornar404_cuandoCamionNoExiste() throws Exception {
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.DESHABILITADO, null);
    when(camionesService.cambiarEstado(eq(ID), any()))
        .thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(
            patch("/api/camiones/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  // ===================== DELETE /api/camiones/{id} =====================

  @Test
  void darDeBaja_deberiaRetornar204_cuandoCamionExiste() throws Exception {
    doNothing().when(camionesService).darDeBaja(ID);

    mockMvc.perform(delete("/api/camiones/" + ID)).andExpect(status().isNoContent());
  }

  @Test
  void darDeBaja_deberiaRetornar404_cuandoCamionNoExiste() throws Exception {
    doThrow(new RecursoNoEncontradoException(ID)).when(camionesService).darDeBaja(ID);

    mockMvc.perform(delete("/api/camiones/" + ID)).andExpect(status().isNotFound());
  }

  @Test
  void darDeBaja_deberiaRetornar400_cuandoCamionEstaEnRuta() throws Exception {
    doThrow(new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA))
        .when(camionesService)
        .darDeBaja(ID);

    mockMvc.perform(delete("/api/camiones/" + ID)).andExpect(status().isBadRequest());
  }
}
