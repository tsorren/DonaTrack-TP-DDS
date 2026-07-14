package grupo5.logistica.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.logistica.controllers.impl.ChoferesController;
import grupo5.logistica.dto.choferes.CambioEstadoChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.services.IChoferesService;
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
class ChoferesControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private IChoferesService choferesService;
  @InjectMocks private ChoferesController controller;

  private static final UUID ID = UUID.randomUUID();

  private static final ChoferResponseDTO RESPONSE_DTO =
      new ChoferResponseDTO(
          ID, "Juan", "Perez", "LIC123456", "1122334455", EstadoChofer.DISPONIBLE, null);

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  // ===================== POST /api/choferes =====================

  @Test
  void crear_deberiaRetornar201_cuandoDatosValidos() throws Exception {
    ChoferRequestDTO request = new ChoferRequestDTO("Juan", "Perez", "LIC123456", "1122334455");

    when(choferesService.crear(any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            post("/api/choferes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("Juan"))
        .andExpect(jsonPath("$.apellido").value("Perez"))
        .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
  }

  @Test
  void crear_deberiaRetornar400_cuandoLicenciaEsInvalida() throws Exception {
    ChoferRequestDTO request = new ChoferRequestDTO("Juan", "Perez", "", "1122334455");

    when(choferesService.crear(any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(
            post("/api/choferes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCatalog.ARGUMENTO_INVALIDO.getCode()));
  }

  // ===================== GET /api/choferes =====================

  @Test
  void listar_deberiaRetornar200_conListaDeChoferes() throws Exception {
    when(choferesService.consultarTodos()).thenReturn(List.of(RESPONSE_DTO));

    mockMvc
        .perform(get("/api/choferes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nombre").value("Juan"));
  }

  @Test
  void listar_deberiaRetornar200_conListaVacia() throws Exception {
    when(choferesService.consultarTodos()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/choferes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ===================== GET /api/choferes/{id} =====================

  @Test
  void consultarPorId_deberiaRetornar200_cuandoChoferExiste() throws Exception {
    when(choferesService.consultarPorId(ID)).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(get("/api/choferes/" + ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @Test
  void consultarPorId_deberiaRetornar404_cuandoChoferNoExiste() throws Exception {
    when(choferesService.consultarPorId(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(get("/api/choferes/" + ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(ID.toString()));
  }

  // ===================== PATCH /api/choferes/{id}/estado =====================

  @Test
  void cambiarEstado_deberiaRetornar200_cuandoTransicionValida() throws Exception {
    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.DESHABILITADO, "Licencia vencida");

    ChoferResponseDTO response =
        new ChoferResponseDTO(
            ID, "Juan", "Perez", "LIC123456", "1122334455", EstadoChofer.DESHABILITADO, null);

    when(choferesService.cambiarEstado(eq(ID), any())).thenReturn(response);

    mockMvc
        .perform(
            patch("/api/choferes/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("DESHABILITADO"));
  }

  @Test
  void cambiarEstado_deberiaRetornar400_cuandoTransicionEsInvalida() throws Exception {
    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.EN_RUTA, null);

    when(choferesService.cambiarEstado(eq(ID), any()))
        .thenThrow(new ValidationException(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA));

    mockMvc
        .perform(
            patch("/api/choferes/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cambiarEstado_deberiaRetornar404_cuandoChoferNoExiste() throws Exception {
    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.DESHABILITADO, "Licencia vencida");

    when(choferesService.cambiarEstado(eq(ID), any()))
        .thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(
            patch("/api/choferes/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  // ===================== DELETE /api/choferes/{id} =====================

  @Test
  void darDeBaja_deberiaRetornar204_cuandoChoferExiste() throws Exception {
    doNothing().when(choferesService).darDeBaja(ID);

    mockMvc.perform(delete("/api/choferes/" + ID)).andExpect(status().isNoContent());
  }

  @Test
  void darDeBaja_deberiaRetornar404_cuandoChoferNoExiste() throws Exception {
    doThrow(new RecursoNoEncontradoException(ID)).when(choferesService).darDeBaja(ID);

    mockMvc.perform(delete("/api/choferes/" + ID)).andExpect(status().isNotFound());
  }

  @Test
  void darDeBaja_deberiaRetornar400_cuandoNoPuedeDarseDeBaja() throws Exception {
    doThrow(new ValidationException(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA))
        .when(choferesService)
        .darDeBaja(ID);

    mockMvc.perform(delete("/api/choferes/" + ID)).andExpect(status().isBadRequest());
  }
}
