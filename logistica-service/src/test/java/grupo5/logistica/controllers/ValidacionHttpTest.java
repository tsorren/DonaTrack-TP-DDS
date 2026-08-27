package grupo5.logistica.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.logistica.controllers.impl.CamionesController;
import grupo5.logistica.controllers.impl.ChoferesController;
import grupo5.logistica.controllers.impl.EntregasController;
import grupo5.logistica.controllers.impl.PlanificacionController;
import grupo5.logistica.controllers.impl.RutasController;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.rutas.AgregarEntregaRutaRequestDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import grupo5.logistica.services.ICamionesService;
import grupo5.logistica.services.IChoferesService;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.IRutasService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Tests de integración HTTP: verifica que @Valid + GlobalExceptionHandler producen los códigos de
 * estado correctos (201, 204, 400, 404) con el pipe de Bean Validation activo.
 */
class ValidacionHttpTest {

  private MockMvc camionMvc;
  private MockMvc choferMvc;
  private MockMvc entregaMvc;
  private MockMvc rutaMvc;
  private MockMvc planificacionMvc;

  private ICamionesService camionesService;
  private IChoferesService choferesService;
  private IEntregasService entregasService;
  private IRutasService rutasService;
  private IPlanificacionService planificacionService;

  private ObjectMapper objectMapper;

  private static final UUID ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
    validatorFactory.afterPropertiesSet();

    camionesService = mock(ICamionesService.class);
    choferesService = mock(IChoferesService.class);
    entregasService = mock(IEntregasService.class);
    rutasService = mock(IRutasService.class);
    planificacionService = mock(IPlanificacionService.class);

    camionMvc =
        MockMvcBuilders.standaloneSetup(new CamionesController(camionesService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validatorFactory)
            .build();
    choferMvc =
        MockMvcBuilders.standaloneSetup(new ChoferesController(choferesService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validatorFactory)
            .build();
    entregaMvc =
        MockMvcBuilders.standaloneSetup(new EntregasController(entregasService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validatorFactory)
            .build();
    rutaMvc =
        MockMvcBuilders.standaloneSetup(new RutasController(rutasService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validatorFactory)
            .build();
    planificacionMvc =
        MockMvcBuilders.standaloneSetup(new PlanificacionController(planificacionService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validatorFactory)
            .build();

    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  // ===================== 201 — Creacion =====================

  @Test
  void crearCamion_conDatosValidos_retorna201() throws Exception {
    CamionRequestDTO request = new CamionRequestDTO("AB123CD", 20f, 3f, 5000f);
    when(camionesService.crear(any())).thenReturn(mock(CamionResponseDTO.class));

    camionMvc
        .perform(
            post("/api/camiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void crearChofer_conDatosValidos_retorna201() throws Exception {
    ChoferRequestDTO request = new ChoferRequestDTO("Ada", "Lovelace", "LIC-1", "1111");
    when(choferesService.crear(any())).thenReturn(mock(ChoferResponseDTO.class));

    choferMvc
        .perform(
            post("/api/choferes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  // ===================== 204 — Baja =====================

  @Test
  void darDeBajaCamion_retorna204() throws Exception {
    camionMvc.perform(delete("/api/camiones/" + ID)).andExpect(status().isNoContent());
  }

  @Test
  void darDeBajaChofer_retorna204() throws Exception {
    choferMvc.perform(delete("/api/choferes/" + ID)).andExpect(status().isNoContent());
  }

  // ===================== 400 — Bean Validation @Valid =====================

  @Test
  void crearCamion_patenteNula_retorna400PorValidacion() throws Exception {
    CamionRequestDTO request = new CamionRequestDTO(null, 20f, 3f, 5000f);

    camionMvc
        .perform(
            post("/api/camiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void crearChofer_nombreNulo_retorna400PorValidacion() throws Exception {
    ChoferRequestDTO request = new ChoferRequestDTO(null, "Lovelace", "LIC-1", "1111");

    choferMvc
        .perform(
            post("/api/choferes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void crearEntrega_destinoNulo_retorna400PorValidacion() throws Exception {
    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null, 10f, 2f);

    entregaMvc
        .perform(
            post("/api/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void agregarEntregaARuta_entregaIdNulo_retorna400PorValidacion() throws Exception {
    AgregarEntregaRutaRequestDTO request = new AgregarEntregaRutaRequestDTO(null);

    rutaMvc
        .perform(
            post("/api/rutas/" + ID + "/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void procesarCallback_solicitudIdNula_retorna400PorValidacion() throws Exception {
    CallbackPlanificacionRequestDTO request =
        new CallbackPlanificacionRequestDTO(null, null, "OK", null);

    planificacionMvc
        .perform(
            post("/api/logistica/resultados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ===================== 400 — UUID malformado en @PathVariable =====================

  @Test
  void consultarCamionPorId_uuidMalformado_retorna400() throws Exception {
    camionMvc.perform(get("/api/camiones/no-es-un-uuid")).andExpect(status().isBadRequest());
  }

  @Test
  void consultarChoferPorId_uuidMalformado_retorna400() throws Exception {
    choferMvc.perform(get("/api/choferes/no-es-un-uuid")).andExpect(status().isBadRequest());
  }

  // ===================== 400 — JSON malformado =====================

  @Test
  void crearCamion_jsonMalformado_retorna400() throws Exception {
    camionMvc
        .perform(
            post("/api/camiones").contentType(MediaType.APPLICATION_JSON).content("{malformed"))
        .andExpect(status().isBadRequest());
  }

  // ===================== Confirmacion de status codes (verificacion adicional)
  // =====================

  @Test
  void crearEntrega_conDatosValidos_retorna201() throws Exception {
    var dir =
        new grupo5.logistica.dto.rutas.DireccionDTO(
            "Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    CrearEntregaRequestDTO request =
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), dir, 10f, 2f);
    when(entregasService.crear(any())).thenReturn(mock(EntregaResponseDTO.class));

    entregaMvc
        .perform(
            post("/api/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void agregarEntregaARuta_conDatosValidos_retorna201() throws Exception {
    AgregarEntregaRutaRequestDTO request = new AgregarEntregaRutaRequestDTO(UUID.randomUUID());
    when(rutasService.agregarEntrega(any(), any())).thenReturn(mock(RutaResponseDTO.class));

    rutaMvc
        .perform(
            post("/api/rutas/" + ID + "/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }
}
