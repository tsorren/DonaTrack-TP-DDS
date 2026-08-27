package grupo5.incentivos.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.services.IGestionDonanteService;
import grupo5.incentivos.services.IInsigniasService;
import grupo5.incentivos.services.IMetricasIncentivosService;
import grupo5.incentivos.services.IMisionesDonacionService;
import grupo5.incentivos.services.IRankingService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ControllersWebMvcValidationTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private IGestionDonanteService gestionDonanteService;
  @Mock private IMisionesDonacionService misionesDonacionService;
  @Mock private IInsigniasService insigniasService;
  @Mock private IMetricasIncentivosService metricasIncentivosService;
  @Mock private IRankingService rankingService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    DonanteIncentivosController donanteController =
        new DonanteIncentivosController(gestionDonanteService);
    MisionesDonacionController misionesController =
        new MisionesDonacionController(misionesDonacionService);
    InsigniasController insigniasController = new InsigniasController(insigniasService);
    MetricasIncentivosController metricasController =
        new MetricasIncentivosController(metricasIncentivosService);
    RankingController rankingController = new RankingController(rankingService);

    mockMvc =
        MockMvcBuilders.standaloneSetup(
                donanteController,
                misionesController,
                insigniasController,
                metricasController,
                rankingController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new TraceResponseHeaderFilter())
            .build();
  }

  @Test
  void registrarDonante_cuandoEsValido_deberiaRetornar201CreatedYHeaderTraceId() throws Exception {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    RegistrarDonanteRequest request =
        new RegistrarDonanteRequest(donanteId, personaId, "Carlos Sanchez");

    when(gestionDonanteService.registrarDonante(any()))
        .thenReturn(new DonanteRegistradoDTO(donanteId, "COLABORADOR"));

    mockMvc
        .perform(
            post("/api/incentivos/donantes/" + donanteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.donanteId").value(donanteId.toString()));
  }

  @Test
  void registrarDonante_conCamposInvalidos_deberiaRetornar400BadRequest() throws Exception {
    UUID donanteId = UUID.randomUUID();
    RegistrarDonanteRequest request = new RegistrarDonanteRequest(null, null, "  ");

    mockMvc
        .perform(
            post("/api/incentivos/donantes/" + donanteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void registrarDonante_conIdPathDiferenteDeBody_deberiaRetornar400BadRequest() throws Exception {
    UUID pathId = UUID.randomUUID();
    UUID bodyId = UUID.randomUUID();
    RegistrarDonanteRequest request =
        new RegistrarDonanteRequest(bodyId, UUID.randomUUID(), "Carlos Sanchez");

    mockMvc
        .perform(
            post("/api/incentivos/donantes/" + pathId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void modificarDonante_conNombreEnBlanco_deberiaRetornar400BadRequest() throws Exception {
    UUID donanteId = UUID.randomUUID();
    ModificarDonanteRequest request = new ModificarDonanteRequest("   ");

    mockMvc
        .perform(
            patch("/api/incentivos/donantes/" + donanteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void darDeBajaDonante_cuandoExiste_deberiaRetornar204NoContent() throws Exception {
    UUID donanteId = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/incentivos/donantes/" + donanteId))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void procesarDonacion_conFechaFutura_deberiaRetornar400BadRequest() throws Exception {
    NuevaDonacionRequest request =
        new NuevaDonacionRequest(
            UUID.randomUUID(), List.of("alimentos"), 5, LocalDate.now().plusDays(5));

    mockMvc
        .perform(
            post("/api/incentivos/donaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void procesarDonacion_conDonanteInexistente_deberiaRetornar404NotFound() throws Exception {
    UUID donanteId = UUID.randomUUID();
    NuevaDonacionRequest request =
        new NuevaDonacionRequest(donanteId, List.of("alimentos"), 5, LocalDate.now());

    doThrow(new BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO))
        .when(misionesDonacionService)
        .procesarDonacion(any());

    mockMvc
        .perform(
            post("/api/incentivos/donaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.code").value(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO.getCode()));
  }

  @Test
  void procesarDonacionExitosa_conOrganizacionNula_deberiaRetornar400BadRequest() throws Exception {
    DonacionExitosaRequest request = new DonacionExitosaRequest(UUID.randomUUID(), null);

    mockMvc
        .perform(
            post("/api/incentivos/donaciones/exitosa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void obtenerUltimoRanking_cuandoNoExiste_deberiaRetornar204NoContent() throws Exception {
    when(rankingService.obtenerUltimoRanking()).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/incentivos/ranking/ultimo")).andExpect(status().isNoContent());
  }

  @Test
  void obtenerPosicionDonante_conPeriodoInvalido_deberiaRetornar400BadRequest() throws Exception {
    UUID donanteId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/incentivos/ranking/posicion/" + donanteId + "?periodo=2026-99"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void endpoint_conUuidMalformadoEnPath_deberiaRetornar400BadRequest() throws Exception {
    mockMvc
        .perform(get("/api/incentivos/donantes/uuid-totalmente-invalido/metricas"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }
}
