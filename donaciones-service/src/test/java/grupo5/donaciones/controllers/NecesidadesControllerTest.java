package grupo5.donaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.donaciones.controllers.impl.NecesidadesController;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.services.INecesidadesService;
import java.time.LocalDate;
import java.time.Month;
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
class NecesidadesControllerTest {

  private MockMvc mockMvc;

  @Mock private INecesidadesService necesidadesService;

  @InjectMocks private NecesidadesController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new TraceResponseHeaderFilter())
            .build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void crearNecesidad_deberiaRetornarCreatedYDto() throws Exception {
    UUID subcategoriaId = UUID.randomUUID();
    NecesidadDTO dto = DTOFixtures.necesidadInput(subcategoriaId);

    when(necesidadesService.guardar(any(NecesidadDTO.class))).thenReturn(dto);

    mockMvc
        .perform(
            post("/api/necesidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.tipo").value("ALIMENTO"))
        .andExpect(jsonPath("$.descripcion").value("Necesidad urgente"));
  }

  @Test
  void crearNecesidad_conCantidadInvalida_deberiaRetornarBadRequest() throws Exception {
    UUID subcategoriaId = UUID.randomUUID();
    NecesidadDTO dto = DTOFixtures.necesidadInput("ALIMENTO", subcategoriaId, -5);

    mockMvc
        .perform(
            post("/api/necesidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("cantidadNecesitada"));
  }

  @Test
  void listarNecesidades_sinFiltros_deberiaRetornarOkYLista() throws Exception {
    UUID entidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();

    NecesidadDTO dto =
        new NecesidadDTO(
            UUID.randomUUID(),
            "EXTRAORDINARIA",
            entidadId,
            subcategoriaId,
            5,
            "Ropa abrigo",
            false,
            LocalDate.of(2026, Month.JUNE, 1),
            null);

    when(necesidadesService.listarConFiltros(null, null)).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/necesidades"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].tipo").value("EXTRAORDINARIA"));
  }

  @Test
  void listarNecesidades_conFiltros_deberiaRetornarOkYListaFiltrada() throws Exception {
    UUID entidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();
    String tipo = "RECURRENTE";

    NecesidadDTO dto =
        new NecesidadDTO(
            UUID.randomUUID(),
            tipo,
            entidadId,
            subcategoriaId,
            10,
            "Leche",
            false,
            LocalDate.of(2026, Month.JUNE, 1),
            LocalDate.of(2026, Month.AUGUST, 1));

    when(necesidadesService.listarConFiltros(entidadId, tipo)).thenReturn(List.of(dto));

    mockMvc
        .perform(
            get("/api/necesidades").param("entidadId", entidadId.toString()).param("tipo", tipo))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].descripcion").value("Leche"));
  }

  @Test
  void obtenerNecesidad_deberiaRetornarOkYDto() throws Exception {
    UUID id = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();

    NecesidadDTO dto =
        new NecesidadDTO(
            id,
            "EXTRAORDINARIA",
            entidadId,
            subcategoriaId,
            5,
            "Ropa abrigo",
            false,
            LocalDate.of(2026, Month.JUNE, 1),
            null);

    when(necesidadesService.obtenerPorId(id)).thenReturn(dto);

    mockMvc
        .perform(get("/api/necesidades/{id}", id))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.descripcion").value("Ropa abrigo"));
  }
}
