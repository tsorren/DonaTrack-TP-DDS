package grupo5.logistica.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.logistica.controllers.impl.EntregasController;
import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.ConfirmarRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.entregas.RegresarAlDepositoRequestDTO;
import grupo5.logistica.dto.entregas.ReportarNoRecepcionRequestDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.services.IEntregasService;
import java.time.LocalDateTime;
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
class EntregasControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private IEntregasService entregasService;
  @InjectMocks private EntregasController controller;

  private static final UUID ID = UUID.randomUUID();
  private static final UUID DONACION_ID = UUID.randomUUID();
  private static final UUID BENEFICIARIA_ID = UUID.randomUUID();
  private static final UUID RUTA_ID = UUID.randomUUID();

  private static final DireccionDTO DIRECCION =
      new DireccionDTO(
          "Av. Siempre Viva", 742, null, null, "1000", "CABA", "Buenos Aires", "Argentina");

  private static final EntregaResponseDTO RESPONSE_DTO =
      new EntregaResponseDTO(
          ID,
          RUTA_ID,
          DONACION_ID,
          BENEFICIARIA_ID,
          DIRECCION,
          EstadoEntrega.PENDIENTE,
          null,
          null,
          null,
          10f,
          2f,
          List.of());

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  // ===================== POST /api/entregas =====================

  @Test
  void crear_deberiaRetornar201_cuandoDatosValidos() throws Exception {

    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(DONACION_ID, BENEFICIARIA_ID, DIRECCION, 10f, 2f);

    when(entregasService.crear(any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            post("/api/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @Test
  void crear_deberiaRetornar400_cuandoRequestEsInvalido() throws Exception {

    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(DONACION_ID, BENEFICIARIA_ID, DIRECCION, 10f, 2f);

    when(entregasService.crear(any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_NULO));

    mockMvc
        .perform(
            post("/api/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ===================== GET /api/entregas =====================

  @Test
  void listar_deberiaRetornar200_conEntregas() throws Exception {

    when(entregasService.listar()).thenReturn(List.of(RESPONSE_DTO));

    mockMvc
        .perform(get("/api/entregas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(ID.toString()));
  }

  @Test
  void listar_deberiaRetornar200_conListaVacia() throws Exception {

    when(entregasService.listar()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/entregas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ===================== GET /api/entregas/{id} =====================

  @Test
  void obtenerPorId_deberiaRetornar200_cuandoExiste() throws Exception {

    when(entregasService.obtenerPorId(ID)).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(get("/api/entregas/" + ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @Test
  void obtenerPorId_deberiaRetornar404_cuandoNoExiste() throws Exception {

    when(entregasService.obtenerPorId(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(get("/api/entregas/" + ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(ID.toString()));
  }

  // ===================== PATCH confirmar recepción =====================

  @Test
  void confirmarRecepcion_deberiaRetornar200() throws Exception {

    ConfirmarRecepcionRequestDTO request = new ConfirmarRecepcionRequestDTO("chofer");

    when(entregasService.confirmarRecepcion(eq(ID), any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/recepciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  // ===================== PATCH foto recepción =====================

  @Test
  void adjuntarFotoRecepcion_deberiaRetornar200() throws Exception {

    AdjuntarFotoRecepcionRequestDTO request =
        new AdjuntarFotoRecepcionRequestDTO("https://foto.com/foto.jpg");

    when(entregasService.adjuntarFotoRecepcion(eq(ID), any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/fotos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  // ===================== PATCH no recibida =====================

  @Test
  void reportarNoRecepcion_deberiaRetornar200() throws Exception {

    ReportarNoRecepcionRequestDTO request =
        new ReportarNoRecepcionRequestDTO("chofer", "No había nadie", true);

    when(entregasService.reportarNoRecepcion(eq(ID), any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/reportes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  // ===================== PATCH regresar depósito =====================

  @Test
  void regresarAlDeposito_deberiaRetornar200() throws Exception {

    RegresarAlDepositoRequestDTO request = new RegresarAlDepositoRequestDTO("chofer");

    when(entregasService.regresarAlDeposito(eq(ID), any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/regresos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  // ===================== GET historial =====================

  @Test
  void obtenerHistorial_deberiaRetornar200() throws Exception {

    CambioEstadoEntregaResponseDTO cambio =
        new CambioEstadoEntregaResponseDTO(
            EstadoEntrega.PENDIENTE, EstadoEntrega.ENTREGADA, LocalDateTime.now(), "chofer");

    when(entregasService.obtenerHistorial(ID)).thenReturn(List.of(cambio));

    mockMvc
        .perform(get("/api/entregas/" + ID + "/historial"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].actor").value("chofer"));
  }

  @Test
  void obtenerHistorial_deberiaRetornar404_cuandoEntregaNoExiste() throws Exception {

    when(entregasService.obtenerHistorial(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc.perform(get("/api/entregas/" + ID + "/historial")).andExpect(status().isNotFound());
  }
}
