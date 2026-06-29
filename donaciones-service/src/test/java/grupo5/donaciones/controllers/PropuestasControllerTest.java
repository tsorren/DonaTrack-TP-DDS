package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.donaciones.controllers.impl.PropuestasController;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.services.impl.PropuestaService;
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
class PropuestasControllerTest {

  private MockMvc mockMvc;

  @Mock private PropuestaService propuestaService;

  @InjectMocks private PropuestasController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void listar_deberiaRetornarOkYListaPropuestas() throws Exception {
    UUID id = UUID.randomUUID();
    NecesidadResumenDTO necesidad =
        new NecesidadResumenDTO(UUID.randomUUID(), "Necesidad abrigo", 10);
    PropuestaDTO dto = new PropuestaDTO(id, EstadoPropuesta.APROBADA, necesidad, List.of());

    when(propuestaService.listarPropuestas()).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/asignaciones/propuestas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].estado").value("APROBADA"))
        .andExpect(jsonPath("$[0].necesidad.descripcion").value("Necesidad abrigo"));
  }

  @Test
  void actualizarEstado_deberiaRetornarOk_CuandoRequestEsValido() throws Exception {
    UUID id = UUID.randomUUID();
    ActualizarEstadoRequestDTO request =
        new ActualizarEstadoRequestDTO(EstadoPropuesta.APROBADA, null);

    doNothing().when(propuestaService).actualizarEstado(id, EstadoPropuesta.APROBADA);

    mockMvc
        .perform(
            put("/api/asignaciones/propuestas/" + id + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(propuestaService, times(1)).actualizarEstado(id, EstadoPropuesta.APROBADA);
  }
}
