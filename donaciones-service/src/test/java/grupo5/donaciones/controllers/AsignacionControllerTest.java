package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.services.PropuestaService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AsignacionControllerTest {

  private MockMvc mockMvc;

  @Mock private PropuestaService propuestaService;

  @InjectMocks private AsignacionController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void ejecutar_deberiaRetornarCreatedYPropuestas() throws Exception {
    Propuesta propuestaMock = mock(Propuesta.class);
    when(propuestaService.ejecutarAsignacion()).thenReturn(List.of(propuestaMock));

    mockMvc
        .perform(post("/api/asignaciones/ejecuciones/ejecuciones"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.size()").value(1));
  }

  @Test
  void historial_deberiaRetornarOkYListaEjecuciones() throws Exception {
    EjecucionAsignacionDTO dto = new EjecucionAsignacionDTO();
    dto.setCantidadPropuestasGeneradas(3);

    when(propuestaService.historialEjecuciones()).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/asignaciones/ejecuciones/ejecuciones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].cantidadPropuestasGeneradas").value(3));
  }
}
