package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.DonacionEnPropuestasDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.FiltroPropuestas;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.dto.propuestas.ResumenPropuestasDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import grupo5.donaciones.services.ParametrosListado;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PropuestaDeAsignacionControllerTest extends AbstractDonacionesWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void ejecutar_deberiaRetornar201YPropuestasGeneradas() throws Exception {
    when(propuestaDeAsignacionService.ejecutarAsignacion()).thenReturn(List.of());

    mockMvc
        .perform(post("/api/asignaciones/ejecuciones"))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$").isArray());

    verify(propuestaDeAsignacionService, times(1)).ejecutarAsignacion();
  }

  @Test
  void historial_deberiaRetornar200YHistorialEjecuciones() throws Exception {
    EjecucionAsignacionDTO dto = new EjecucionAsignacionDTO();
    dto.setId(UUID.randomUUID());
    dto.setFechaEjecucion(LocalDateTime.now());
    dto.setCantidadPropuestasGeneradas(3);

    when(propuestaDeAsignacionService.historialEjecuciones(any()))
        .thenReturn(new Pagina<>(List.of(dto), 3));

    mockMvc
        .perform(get("/api/asignaciones/ejecuciones?page=0&size=1"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(header().string("X-Total-Count", "3"))
        .andExpect(jsonPath("$[0].cantidadPropuestasGeneradas").value(3));

    verify(propuestaDeAsignacionService, times(1))
        .historialEjecuciones(new ParametrosListado(0, 1, null, null));
  }

  @Test
  void listar_deberiaRetornar200YListaDePropuestas() throws Exception {
    UUID propId = UUID.randomUUID();
    PropuestaDTO dto =
        new PropuestaDTO(
            propId, EstadoPropuesta.PENDIENTE, LocalDateTime.now(), null, List.of(), null, null);

    when(propuestaDeAsignacionService.listarPropuestas(any(), any()))
        .thenReturn(new Pagina<>(List.of(dto), 45));

    mockMvc
        .perform(get("/api/asignaciones/propuestas"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(header().string("X-Total-Count", "45"))
        .andExpect(jsonPath("$[0].id").value(propId.toString()))
        .andExpect(jsonPath("$[0].sugeridaPor").isArray());

    verify(propuestaDeAsignacionService, times(1))
        .listarPropuestas(FiltroPropuestas.ninguno(), ParametrosListado.primeraPagina());
  }

  // A4 y A6 (spec R11-24): los filtros repetibles llegan al servicio
  @Test
  void listar_conFiltros_losPasaAlServicio() throws Exception {
    UUID donacion = UUID.randomUUID();
    when(propuestaDeAsignacionService.listarPropuestas(any(), any()))
        .thenReturn(new Pagina<>(List.of(), 0));

    mockMvc
        .perform(
            get(
                "/api/asignaciones/propuestas?estado=PENDIENTE&situacion=EN_CONFLICTO"
                    + "&situacion=SIN_STOCK&algoritmo=AMBOS&orden=reciente&page=2&size=10"
                    + "&donacion="
                    + donacion))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "0"));

    verify(propuestaDeAsignacionService)
        .listarPropuestas(
            new FiltroPropuestas(
                List.of(EstadoPropuesta.PENDIENTE),
                List.of(SituacionPropuesta.EN_CONFLICTO, SituacionPropuesta.SIN_STOCK),
                "AMBOS",
                donacion),
            new ParametrosListado(2, 10, null, "reciente"));
  }

  // A4: un valor desconocido es un error del pedido
  @Test
  void listar_conUnaSituacionDesconocida_respondeBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/asignaciones/propuestas?situacion=OTRA"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void resumen_devuelveLosTotales() throws Exception {
    when(propuestaDeAsignacionService.resumen()).thenReturn(new ResumenPropuestasDTO(11, 8, 2, 1));

    mockMvc
        .perform(get("/api/asignaciones/propuestas/resumen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendientes").value(11))
        .andExpect(jsonPath("$.libres").value(8))
        .andExpect(jsonPath("$.enConflicto").value(2))
        .andExpect(jsonPath("$.sinStock").value(1));
  }

  @Test
  void donaciones_devuelveLaPaginaConSuTotal() throws Exception {
    UUID categoria = UUID.randomUUID();
    when(propuestaDeAsignacionService.donacionesEnPendientes(any(), eq(true), any()))
        .thenReturn(new Pagina<>(List.of(new DonacionEnPropuestasDTO(null, 25, 50, 2, false)), 7));

    mockMvc
        .perform(
            get(
                "/api/asignaciones/donaciones?soloEnConflicto=true&size=10&categoriaId="
                    + categoria))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "7"))
        .andExpect(jsonPath("$[0].pedidaEnPendientes").value(50))
        .andExpect(jsonPath("$[0].alcanza").value(false));

    verify(propuestaDeAsignacionService)
        .donacionesEnPendientes(categoria, true, new ParametrosListado(null, 10, null, null));
  }

  @Test
  void actualizarEstado_deberiaRetornar200() throws Exception {
    UUID propId = UUID.randomUUID();
    ActualizarEstadoRequestDTO requestDTO =
        DTOFixtures.actualizarEstadoPropuestaInput(EstadoPropuesta.APROBADA);

    mockMvc
        .perform(
            put("/api/asignaciones/propuestas/" + propId + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));

    verify(propuestaDeAsignacionService, times(1))
        .actualizarEstado(propId, EstadoPropuesta.APROBADA, "SISTEMA");
  }

  @Test
  void actualizarEstado_conEstadoNulo_deberiaRetornarBadRequest() throws Exception {
    UUID propId = UUID.randomUUID();
    ActualizarEstadoRequestDTO requestDTO = new ActualizarEstadoRequestDTO(null, null);

    mockMvc
        .perform(
            put("/api/asignaciones/propuestas/" + propId + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("estado"));
  }
}
