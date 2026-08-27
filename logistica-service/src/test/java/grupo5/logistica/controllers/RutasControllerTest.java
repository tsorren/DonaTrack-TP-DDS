package grupo5.logistica.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
import grupo5.logistica.controllers.impl.RutasController;
import grupo5.logistica.dto.rutas.*;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.services.IRutasService;
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
class RutasControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private IRutasService rutasService;
  @InjectMocks private RutasController controller;

  private static final UUID ID = UUID.randomUUID();
  private static final UUID CAMION_ID = UUID.randomUUID();
  private static final UUID CHOFER_ID = UUID.randomUUID();
  private static final UUID ENTREGA_ID = UUID.randomUUID();

  private static final RutaResponseDTO RESPONSE_DTO =
      new RutaResponseDTO(
          ID,
          LocalDate.now(),
          List.of(ENTREGA_ID),
          CHOFER_ID,
          CAMION_ID,
          EstadoRuta.PENDIENTE,
          null,
          null,
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

  // ===================== GET /api/rutas =====================

  @Test
  void listar_deberiaRetornar200_cuandoNoHayFiltro() throws Exception {
    when(rutasService.listar()).thenReturn(List.of(RESPONSE_DTO));

    mockMvc
        .perform(get("/api/rutas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(ID.toString()))
        .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
  }

  @Test
  void listar_deberiaRetornar200_cuandoSeFiltraPorCamion() throws Exception {
    when(rutasService.listarPorCamion(CAMION_ID)).thenReturn(List.of(RESPONSE_DTO));

    mockMvc
        .perform(get("/api/rutas").param("camionId", CAMION_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].camionId").value(CAMION_ID.toString()));
  }

  // ===================== GET /api/rutas/{id} =====================

  @Test
  void obtenerPorId_deberiaRetornar200_cuandoRutaExiste() throws Exception {
    when(rutasService.obtenerPorId(ID)).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(get("/api/rutas/" + ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @Test
  void obtenerPorId_deberiaRetornar404_cuandoRutaNoExiste() throws Exception {
    when(rutasService.obtenerPorId(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(get("/api/rutas/" + ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(ID.toString()));
  }

  // ===================== GET /api/rutas/{id}/entregas =====================

  @Test
  void obtenerConEntregas_deberiaRetornar200() throws Exception {

    RutaConEntregasResponseDTO response =
        new RutaConEntregasResponseDTO(
            ID,
            LocalDate.now(),
            List.of(),
            CHOFER_ID,
            CAMION_ID,
            EstadoRuta.PENDIENTE,
            null,
            null,
            null);

    when(rutasService.obtenerConEntregas(ID)).thenReturn(response);

    mockMvc
        .perform(get("/api/rutas/" + ID + "/entregas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @Test
  void obtenerConEntregas_deberiaRetornar404_cuandoRutaNoExiste() throws Exception {

    when(rutasService.obtenerConEntregas(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(get("/api/rutas/" + ID + "/entregas"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(ID.toString()));
  }

  // ===================== POST /api/rutas/{id}/entregas =====================

  @Test
  void agregarEntrega_deberiaRetornar201() throws Exception {

    AgregarEntregaRutaRequestDTO request = new AgregarEntregaRutaRequestDTO(ENTREGA_ID);

    when(rutasService.agregarEntrega(eq(ID), any())).thenReturn(RESPONSE_DTO);

    mockMvc
        .perform(
            post("/api/rutas/" + ID + "/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(ID.toString()));
  }

  @Test
  void agregarEntrega_deberiaRetornar400_cuandoRequestEsInvalido() throws Exception {

    AgregarEntregaRutaRequestDTO request = new AgregarEntregaRutaRequestDTO(null);

    mockMvc
        .perform(
            post("/api/rutas/" + ID + "/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ===================== PATCH /api/rutas/{id}/estado =====================

  @Test
  void cambiarEstado_deberiaRetornar200_alIniciarRuta() throws Exception {
    CambioEstadoRutaRequestDTO request =
        new CambioEstadoRutaRequestDTO(EstadoRuta.EN_TRASLADO, CHOFER_ID, "actor");
    RutaResponseDTO response = mock(RutaResponseDTO.class);

    when(rutasService.cambiarEstado(eq(ID), any())).thenReturn(response);

    mockMvc
        .perform(
            patch("/api/rutas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void cambiarEstado_deberiaRetornar400_cuandoRequestEsInvalido() throws Exception {
    CambioEstadoRutaRequestDTO request =
        new CambioEstadoRutaRequestDTO(EstadoRuta.EN_TRASLADO, UUID.randomUUID(), "chofer");

    when(rutasService.cambiarEstado(eq(ID), any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(
            patch("/api/rutas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
