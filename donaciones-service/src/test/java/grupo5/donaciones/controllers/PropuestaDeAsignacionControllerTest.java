package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.donaciones.controllers.impl.PropuestaDeAsignacionController;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PropuestaDeAsignacionControllerTest {

  private MockMvc mockMvc;
  private IPropuestaDeAsignacionService serviceMock;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    serviceMock = mock(IPropuestaDeAsignacionService.class);
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();

    PropuestaDeAsignacionController controller = new PropuestaDeAsignacionController(serviceMock);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void ejecutar_deberiaRetornar201YPropuestasGeneradas() throws Exception {
    when(serviceMock.ejecutarAsignacion()).thenReturn(List.of());

    mockMvc
        .perform(post("/api/asignaciones/ejecuciones"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$").isArray());

    verify(serviceMock, times(1)).ejecutarAsignacion();
  }

  @Test
  void historial_deberiaRetornar200YHistorialEjecuciones() throws Exception {
    EjecucionAsignacionDTO dto = new EjecucionAsignacionDTO();
    dto.setId(UUID.randomUUID());
    dto.setFechaEjecucion(LocalDateTime.now());
    dto.setCantidadPropuestasGeneradas(3);

    when(serviceMock.historialEjecuciones()).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/asignaciones/ejecuciones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].cantidadPropuestasGeneradas").value(3));

    verify(serviceMock, times(1)).historialEjecuciones();
  }

  @Test
  void listar_deberiaRetornar200YListaDePropuestas() throws Exception {
    UUID propId = UUID.randomUUID();
    PropuestaDTO dto =
        new PropuestaDTO(propId, EstadoPropuesta.PENDIENTE, LocalDateTime.now(), null, List.of());

    when(serviceMock.listarPropuestas()).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/asignaciones/propuestas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(propId.toString()));

    verify(serviceMock, times(1)).listarPropuestas();
  }

  @Test
  void actualizarEstado_deberiaRetornar200() throws Exception {
    UUID propId = UUID.randomUUID();
    ActualizarEstadoRequestDTO requestDTO =
        new ActualizarEstadoRequestDTO(EstadoPropuesta.APROBADA, null);

    mockMvc
        .perform(
            put("/api/asignaciones/propuestas/" + propId + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isOk());

    verify(serviceMock, times(1)).actualizarEstado(propId, EstadoPropuesta.APROBADA);
  }
}
