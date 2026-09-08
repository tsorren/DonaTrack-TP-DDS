package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.donaciones.controllers.impl.EntidadBeneficiariaController;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EntidadBeneficiariaController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
class EntidadBeneficiariaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IEntidadBeneficiariaService service;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void crearEntidad_debeRetornarCreated() throws Exception {
    UUID juridicaId = UUID.randomUUID();
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(juridicaId);
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);

    when(service.crearEntidad(any())).thenReturn(outputMock);

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
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);

    when(service.obtenerEntidad(id)).thenReturn(outputMock);

    mockMvc
        .perform(get("/api/entidades/" + id))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void obtenerTodas_debeRetornarOk() throws Exception {
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);
    when(service.obtenerTodas()).thenReturn(List.of(outputMock));

    mockMvc
        .perform(get("/api/entidades"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void actualizarEntidad_debeRetornarOk() throws Exception {
    UUID id = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(juridicaId);
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);

    when(service.actualizarEntidad(any(UUID.class), any(EntidadBeneficiariaInputDTO.class)))
        .thenReturn(outputMock);

    mockMvc
        .perform(
            put("/api/entidades/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void eliminarEntidad_debeRetornarNoContent() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/entidades/" + id)).andExpect(status().isNoContent());
  }
}
