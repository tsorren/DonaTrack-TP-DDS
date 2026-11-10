package grupo5.donaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.models.entities.necesidades.ResultadoPeriodo;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class NecesidadesControllerTest extends AbstractDonacionesWebMvcTest {

  private static final String CANTIDAD = "$.size()";

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

    when(necesidadesService.listarConFiltros(null, null, null)).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/necesidades"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath(CANTIDAD).value(1))
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

    when(necesidadesService.listarConFiltros(entidadId, tipo, null)).thenReturn(List.of(dto));

    mockMvc
        .perform(
            get("/api/necesidades").param("entidadId", entidadId.toString()).param("tipo", tipo))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath(CANTIDAD).value(1))
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

  @Test
  void actualizarNecesidad_deberiaRetornarOkYDto() throws Exception {
    UUID id = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();

    NecesidadDTO dto =
        new NecesidadDTO(
            id,
            "EXTRAORDINARIA",
            entidadId,
            subcategoriaId,
            10,
            "Ropa abrigo actualizada",
            false,
            LocalDate.of(2026, Month.JUNE, 1),
            null);

    when(necesidadesService.actualizar(any(UUID.class), any(NecesidadDTO.class))).thenReturn(dto);

    mockMvc
        .perform(
            put("/api/necesidades/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.descripcion").value("Ropa abrigo actualizada"))
        .andExpect(jsonPath("$.cantidadNecesitada").value(10));
  }

  @Test
  void eliminarNecesidad_deberiaRetornarNoContent() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/necesidades/{id}", id)).andExpect(status().isNoContent());
  }

  // ===================== GET /api/necesidades/publicas (HB-08) =====================

  @Test
  void listarNecesidadesPublicas_devuelveElDtoPublico() throws Exception {
    UUID entidadId = UUID.randomUUID();
    NecesidadPublicaDTO dto =
        new NecesidadPublicaDTO(
            UUID.randomUUID(),
            entidadId,
            UUID.randomUUID(),
            "Ropa abrigo",
            5,
            "RECURRENTE",
            LocalDate.of(2026, Month.NOVEMBER, 30));
    when(necesidadesService.listarPublicas(entidadId)).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/necesidades/publicas").param("entidadId", entidadId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath(CANTIDAD).value(1))
        .andExpect(jsonPath("$[0].id").value(dto.id().toString()))
        .andExpect(jsonPath("$[0].idEntidad").value(entidadId.toString()))
        .andExpect(jsonPath("$[0].cantidadNecesitada").value(5))
        .andExpect(jsonPath("$[0].fechaFin").value("2026-11-30"))
        .andExpect(jsonPath("$[0].estaSatisfecha").doesNotExist())
        .andExpect(jsonPath("$[0].fechaInicio").doesNotExist());
  }

  @Test
  void listarNecesidadesPublicas_sinEntidad_listaTodasLasVigentes() throws Exception {
    NecesidadPublicaDTO dto =
        new NecesidadPublicaDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Leche",
            3,
            "EXTRAORDINARIA",
            null);
    when(necesidadesService.listarPublicas(null)).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/necesidades/publicas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(CANTIDAD).value(1))
        .andExpect(jsonPath("$[0].tipo").value("EXTRAORDINARIA"))
        .andExpect(jsonPath("$[0].fechaFin").doesNotExist());
  }

  @Test
  void listarNecesidades_porEstado_aceptaVariosSeparadosPorComa() throws Exception {
    when(necesidadesService.listarConFiltros(
            null, null, List.of(EstadoNecesidad.VIGENTE, EstadoNecesidad.CUMPLIDA)))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/api/necesidades").param("estado", "VIGENTE,CUMPLIDA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(CANTIDAD).value(0));
  }

  @Test
  void listarNecesidades_conUnEstadoQueNoExiste_respondeBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/necesidades").param("estado", "ARCHIVADA"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listarPeriodos_deberiaRetornarLaHistoria() throws Exception {
    UUID id = UUID.randomUUID();
    PeriodoNecesidadDTO periodo =
        new PeriodoNecesidadDTO(
            UUID.randomUUID(),
            LocalDate.of(2026, Month.OCTOBER, 1),
            LocalDate.of(2026, Month.NOVEMBER, 1),
            40,
            40,
            35,
            ResultadoPeriodo.INCOMPLETO);
    when(necesidadesService.listarPeriodos(id)).thenReturn(List.of(periodo));

    mockMvc
        .perform(get("/api/necesidades/{id}/periodos", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath(CANTIDAD).value(1))
        .andExpect(jsonPath("$[0].inicio").value("2026-10-01"))
        .andExpect(jsonPath("$[0].recibido").value(35))
        .andExpect(jsonPath("$[0].resultado").value("INCOMPLETO"));
  }
}
