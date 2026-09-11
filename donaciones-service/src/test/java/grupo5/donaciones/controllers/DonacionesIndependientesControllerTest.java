package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class DonacionesIndependientesControllerTest extends AbstractDonacionesWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String ACTOR = "SISTEMA";

  @Test
  void cambiarEstado_DeberiaRetornarOk_CuandoTransicionEsExitosa() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        DTOFixtures.cambioEstadoDIInput(TipoEstadoDonacion.ASIGNACION_REALIZADA);
    DonacionIndependienteResponseDTO response =
        new DonacionIndependienteResponseDTO(
            id,
            UUID.randomUUID(),
            "Descripcion",
            "AsignacionRealizada",
            java.time.LocalDateTime.now(),
            List.of(
                new grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO(
                    "EnDeposito",
                    "AsignacionRealizada",
                    java.time.LocalDateTime.now(),
                    null,
                    ACTOR)),
            List.of(),
            0);

    when(donacionesIndependientesService.cambiarEstado(eq(id), any(), eq(ACTOR)))
        .thenReturn(response);

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.estadoActual").value("AsignacionRealizada"))
        .andExpect(jsonPath("$.historial[0].estadoNuevo").value("AsignacionRealizada"));
  }

  @Test
  void cambiarEstado_DeberiaRetornarBadRequest_CuandoFaltaHeaderActor() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        DTOFixtures.cambioEstadoDIInput(TipoEstadoDonacion.ASIGNACION_REALIZADA);

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void cambiarEstado_DeberiaRetornarBadRequest_CuandoEstadoEsNulo() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(null, null, null, null, null, null);

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("estado"));
  }

  @Test
  void cambiarEstado_DeberiaRetornarNotFound_CuandoRecursoNoExiste() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        DTOFixtures.cambioEstadoDIInput(TipoEstadoDonacion.ASIGNACION_REALIZADA);

    when(donacionesIndependientesService.cambiarEstado(eq(id), any(), eq(ACTOR)))
        .thenThrow(new RecursoNoEncontradoException(id));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.details").value(id.toString()));
  }

  @Test
  void cambiarEstado_DeberiaRetornarConflict_CuandoTransicionEsInvalida() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        DTOFixtures.cambioEstadoDIInput(TipoEstadoDonacion.ENTREGADA);

    when(donacionesIndependientesService.cambiarEstado(eq(id), any(), eq(ACTOR)))
        .thenThrow(new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(
            jsonPath("$.code").value(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA.getCode()));
  }

  @Test
  void cambiarEstado_DeberiaRetornarBadRequest_CuandoArgumentoEsInvalido() throws Exception {
    UUID id = UUID.randomUUID();
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, "", null, null, null, null);

    when(donacionesIndependientesService.cambiarEstado(eq(id), any(), eq(ACTOR)))
        .thenThrow(
            new IllegalArgumentException(
                "La justificación es obligatoria para registrar una entrega fallida."));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.code").value(ErrorCatalog.ARGUMENTO_INVALIDO.getCode()));
  }

  @Test
  void obtenerTodas_DeberiaRetornarOkYLista() throws Exception {
    UUID id = UUID.randomUUID();
    DonacionIndependienteResponseDTO response =
        new DonacionIndependienteResponseDTO(
            id,
            UUID.randomUUID(),
            "Descripcion",
            "EnDeposito",
            java.time.LocalDateTime.now(),
            List.of(),
            List.of(),
            5);

    when(donacionesIndependientesService.obtenerConFiltros(null, null, null))
        .thenReturn(List.of(response));

    mockMvc
        .perform(get("/donaciones-independientes"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].descripcion").value("Descripcion"))
        .andExpect(jsonPath("$[0].estadoActual").value("EnDeposito"))
        .andExpect(jsonPath("$[0].cantidad").value(5));
  }

  @Test
  void obtener_DeberiaRetornarOkYDto() throws Exception {
    UUID id = UUID.randomUUID();
    DonacionIndependienteResponseDTO response =
        new DonacionIndependienteResponseDTO(
            id,
            UUID.randomUUID(),
            "Descripcion",
            "EnDeposito",
            java.time.LocalDateTime.now(),
            List.of(),
            List.of(),
            5);

    when(donacionesIndependientesService.obtener(id)).thenReturn(response);

    mockMvc
        .perform(get("/donaciones-independientes/{id}", id))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.descripcion").value("Descripcion"))
        .andExpect(jsonPath("$.estadoActual").value("EnDeposito"))
        .andExpect(jsonPath("$.cantidad").value(5));
  }

  @Test
  void obtener_DeberiaRetornarNotFound_CuandoNoExiste() throws Exception {
    UUID id = UUID.randomUUID();
    when(donacionesIndependientesService.obtener(id))
        .thenThrow(new RecursoNoEncontradoException(id));

    mockMvc
        .perform(get("/donaciones-independientes/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.details").value(id.toString()));
  }
}
