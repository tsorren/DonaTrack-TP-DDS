package grupo5.logistica.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.model.Request;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.testutils.OpenApiTestUtils;
import grupo5.logistica.dto.entregas.*;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class EntregasControllerTest extends AbstractLogisticaWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

  // ===================== POST /api/entregas =====================

  @Test
  void crear_deberiaRetornar201_cuandoDatosValidos() throws Exception {

    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(DONACION_ID, BENEFICIARIA_ID, DIRECCION, 10f, 2f);

    when(entregasService.crear(any())).thenReturn(RESPONSE_DTO);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/entregas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andReturn();

    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiTestUtils.assertResponseConformsToOpenApi(
        specPath,
        "/api/entregas",
        Request.Method.POST,
        201,
        result.getResponse().getContentAsString());
  }

  @Test
  void crear_deberiaRetornar201_conPisoYDepartamentoNoNulos() throws Exception {
    DireccionDTO direccionConPiso =
        new DireccionDTO(
            "Av. Corrientes", 1234, 5, "A", "1043", "CABA", "Buenos Aires", "Argentina");
    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(DONACION_ID, BENEFICIARIA_ID, direccionConPiso, 10f, 2f);
    EntregaResponseDTO responseConPiso =
        new EntregaResponseDTO(
            ID,
            RUTA_ID,
            DONACION_ID,
            BENEFICIARIA_ID,
            direccionConPiso,
            EstadoEntrega.PENDIENTE,
            null,
            null,
            null,
            10f,
            2f,
            List.of());

    when(entregasService.crear(any())).thenReturn(responseConPiso);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/entregas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.destino.piso").value(5))
            .andExpect(jsonPath("$.destino.departamento").value("A"))
            .andReturn();

    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiTestUtils.assertResponseConformsToOpenApi(
        specPath,
        "/api/entregas",
        Request.Method.POST,
        201,
        result.getResponse().getContentAsString());
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

  // ===================== GET /api/entregas/mapa (RF-02) =====================

  @Test
  void listarMapaPublico_deberiaRetornar200YCumplirContratoOpenApiSinDatosSensibles()
      throws Exception {
    EntregaMapaPublicoResponseDTO punto =
        new EntregaMapaPublicoResponseDTO(
            ID,
            -34.5986,
            -58.4201,
            "CABA",
            "Buenos Aires",
            "Argentina",
            LocalDateTime.of(2026, 10, 22, 14, 30),
            12.5f,
            1.2f);
    when(entregasService.listarMapaPublico()).thenReturn(List.of(punto));

    MvcResult result =
        mockMvc
            .perform(get("/api/entregas/mapa"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].idEntrega").value(ID.toString()))
            .andExpect(jsonPath("$[0].latitud").value(-34.5986))
            .andExpect(jsonPath("$[0].longitud").value(-58.4201))
            .andExpect(jsonPath("$[0].calle").doesNotExist())
            .andExpect(jsonPath("$[0].altura").doesNotExist())
            .andExpect(jsonPath("$[0].idBeneficiaria").doesNotExist())
            .andExpect(jsonPath("$[0].idDonacion").doesNotExist())
            .andExpect(jsonPath("$[0].fotoRecepcionUrl").doesNotExist())
            .andExpect(jsonPath("$[0].fotosRecepcion").doesNotExist())
            .andReturn();

    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiTestUtils.assertResponseConformsToOpenApi(
        specPath,
        "/api/entregas/mapa",
        Request.Method.GET,
        200,
        result.getResponse().getContentAsString());
  }

  // ===================== GET /api/entregas/seguimiento/* (RF-09 y RF-14) =====================

  @Test
  void obtenerSeguimientoPorDonacion_deberiaRetornar200YCumplirContratoOpenApi() throws Exception {
    SeguimientoEntregaResponseDTO seguimiento =
        new SeguimientoEntregaResponseDTO(
            ID,
            DONACION_ID,
            EstadoEntrega.EN_TRASLADO,
            true,
            RUTA_ID,
            LocalDate.of(2026, 10, 22),
            EstadoRuta.EN_TRASLADO,
            "AB123CD",
            2,
            3,
            1,
            1,
            LocalDateTime.of(2026, 10, 22, 9, 0),
            null,
            LocalDateTime.of(2026, 10, 22, 10, 15),
            List.of(
                new ParadaSeguimientoDTO(
                    1, EstadoEntrega.ENTREGADA, false, LocalDateTime.of(2026, 10, 22, 10, 15)),
                new ParadaSeguimientoDTO(2, EstadoEntrega.EN_TRASLADO, true, null),
                new ParadaSeguimientoDTO(3, EstadoEntrega.EN_TRASLADO, false, null)));

    when(entregasService.obtenerSeguimientoPorDonacion(DONACION_ID)).thenReturn(seguimiento);

    MvcResult result =
        mockMvc
            .perform(get("/api/entregas/seguimiento/donaciones/" + DONACION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idDonacion").value(DONACION_ID.toString()))
            .andExpect(jsonPath("$.ordenVisita").value(2))
            .andExpect(jsonPath("$.paradasEntregadas").value(1))
            .andExpect(jsonPath("$.paradasProcesadas").value(1))
            .andReturn();

    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiTestUtils.assertResponseConformsToOpenApi(
        specPath,
        "/api/entregas/seguimiento/donaciones/" + DONACION_ID,
        Request.Method.GET,
        200,
        result.getResponse().getContentAsString());
  }

  @Test
  void obtenerSeguimientoPorDonacion_deberiaRetornar404_cuandoNoExiste() throws Exception {
    when(entregasService.obtenerSeguimientoPorDonacion(DONACION_ID))
        .thenThrow(new RecursoNoEncontradoException(DONACION_ID));

    mockMvc
        .perform(get("/api/entregas/seguimiento/donaciones/" + DONACION_ID))
        .andExpect(status().isNotFound());
  }

  @Test
  void listarSeguimientoPorBeneficiaria_deberiaRetornar200YCumplirContratoOpenApi()
      throws Exception {
    SeguimientoEntregaResponseDTO seguimiento =
        new SeguimientoEntregaResponseDTO(
            ID,
            DONACION_ID,
            EstadoEntrega.PENDIENTE,
            false,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            0,
            null,
            null,
            null,
            List.of());

    when(entregasService.listarSeguimientoPorBeneficiaria(BENEFICIARIA_ID))
        .thenReturn(List.of(seguimiento));

    MvcResult result =
        mockMvc
            .perform(get("/api/entregas/seguimiento/beneficiarios/" + BENEFICIARIA_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].idEntrega").value(ID.toString()))
            .andExpect(jsonPath("$[0].rutaAsignada").value(false))
            .andReturn();

    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiTestUtils.assertResponseConformsToOpenApi(
        specPath,
        "/api/entregas/seguimiento/beneficiarios/" + BENEFICIARIA_ID,
        Request.Method.GET,
        200,
        result.getResponse().getContentAsString());
  }

  // ===================== GET /api/entregas/{id} =====================

  @Test
  void obtenerPorId_deberiaRetornar200_cuandoExiste() throws Exception {

    when(entregasService.obtenerPorId(ID)).thenReturn(RESPONSE_DTO);

    MvcResult result =
        mockMvc
            .perform(get("/api/entregas/" + ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andReturn();

    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiTestUtils.assertResponseConformsToOpenApi(
        specPath,
        "/api/entregas/" + ID,
        Request.Method.GET,
        200,
        result.getResponse().getContentAsString());
  }

  @Test
  void obtenerPorId_deberiaRetornar404_cuandoNoExiste() throws Exception {

    when(entregasService.obtenerPorId(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc
        .perform(get("/api/entregas/" + ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.details").value(ID.toString()));
  }

  // ===================== PATCH /api/entregas/{id}/estado =====================

  @Test
  void cambiarEstado_deberiaRetornar200_cuandoEntregada() throws Exception {
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(
            EstadoEntrega.ENTREGADA,
            "chofer",
            null,
            null,
            List.of("https://foto.com/evidencia.jpg"));
    EntregaResponseDTO responseDto = mock(EntregaResponseDTO.class);
    when(entregasService.cambiarEstado(eq(ID), any())).thenReturn(responseDto);
    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void cambiarEstado_deberiaRetornar200_cuandoNoRecibida() throws Exception {
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(
            EstadoEntrega.NO_RECIBIDA, "chofer", "Destinatario ausente", true);
    EntregaResponseDTO responseDto = mock(EntregaResponseDTO.class);
    when(entregasService.cambiarEstado(eq(ID), any())).thenReturn(responseDto);
    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void cambiarEstado_deberiaRetornar200_cuandoRevision() throws Exception {
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.REVISION, "admin", null, null);
    EntregaResponseDTO responseDto = mock(EntregaResponseDTO.class);
    when(entregasService.cambiarEstado(eq(ID), any())).thenReturn(responseDto);
    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void cambiarEstado_deberiaRetornar200_cuandoRegresoAlDeposito() throws Exception {
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.PENDIENTE, "admin", null, null);
    EntregaResponseDTO responseDto = mock(EntregaResponseDTO.class);
    when(entregasService.cambiarEstado(eq(ID), any())).thenReturn(responseDto);
    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void cambiarEstado_deberiaRetornar400_cuandoEstadoEsNulo() throws Exception {
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(null, "chofer", null, null);
    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cambiarEstado_deberiaRetornar400_cuandoActorEsVacio() throws Exception {
    CambioEstadoEntregaRequestDTO request =
        new CambioEstadoEntregaRequestDTO(EstadoEntrega.ENTREGADA, "   ", null, null);
    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ===================== PATCH /api/entregas/{id}/foto =====================

  @Test
  void adjuntarFotoRecepcion_deberiaRetornar200() throws Exception {
    AdjuntarFotoRecepcionRequestDTO request =
        new AdjuntarFotoRecepcionRequestDTO("https://foto.com/foto.jpg");
    EntregaResponseDTO responseDto = mock(EntregaResponseDTO.class);

    when(entregasService.adjuntarFotoRecepcion(eq(ID), any())).thenReturn(responseDto);

    mockMvc
        .perform(
            patch("/api/entregas/" + ID + "/fotos")
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
