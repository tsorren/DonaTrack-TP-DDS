package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.donaciones.controllers.impl.EntidadBeneficiariaController;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
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
class EntidadBeneficiariaControllerTest {

  private MockMvc mockMvc;

  @Mock private IEntidadBeneficiariaService service;

  @InjectMocks private EntidadBeneficiariaController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new TraceResponseHeaderFilter())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void crearEntidad_debeRetornarCreated() throws Exception {
    UUID juridicaId = UUID.randomUUID();
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(juridicaId);

    when(service.crearEntidad(any())).thenReturn(mock(EntidadBeneficiariaOutputDTO.class));

    mockMvc
        .perform(
            post("/api/entidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void crearEntidad_conJuridicaIdNulo_debeRetornarBadRequest() throws Exception {
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(null);

    mockMvc
        .perform(
            post("/api/entidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("juridicaId"));
  }

  @Test
  void obtenerEntidad_debeRetornarOk() throws Exception {
    UUID id = UUID.randomUUID();

    when(service.obtenerEntidad(id)).thenReturn(mock(EntidadBeneficiariaOutputDTO.class));

    mockMvc
        .perform(get("/api/entidades/" + id))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void obtenerTodas_debeRetornarOk() throws Exception {
    when(service.obtenerTodas()).thenReturn(List.of(mock(EntidadBeneficiariaOutputDTO.class)));

    mockMvc
        .perform(get("/api/entidades"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }
}
