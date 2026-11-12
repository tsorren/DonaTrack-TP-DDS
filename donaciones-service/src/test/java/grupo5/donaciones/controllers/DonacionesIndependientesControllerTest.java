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
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.ConteosDeEntidadDTO;
import grupo5.donaciones.dto.donacionesIndependientes.ConteosDeEntidadDTO.EntregadasEnElMes;
import grupo5.donaciones.dto.donacionesIndependientes.ConteosDeEntidadDTO.PorEstado;
import grupo5.donaciones.dto.donacionesIndependientes.ConteosDeEntidadDTO.PorNecesidad;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.services.ParametrosListado;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class DonacionesIndependientesControllerTest extends AbstractDonacionesWebMvcTest {

  private static final String RUTA = "/donaciones-independientes";

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
            0,
            null);

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
            5,
            null);

    when(donacionesIndependientesService.obtenerConFiltros(
            new FiltroDonacionesIndependientes(null, null, null, null, null, null, null, null),
            ParametrosListado.primeraPagina()))
        .thenReturn(new Pagina<>(List.of(response), 120));

    mockMvc
        .perform(get("/donaciones-independientes"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(header().string("X-Total-Count", "120"))
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].descripcion").value("Descripcion"))
        .andExpect(jsonPath("$[0].estadoActual").value("EnDeposito"))
        .andExpect(jsonPath("$[0].cantidad").value(5));
  }

  @Test
  void obtenerTodas_conFiltros_pasaTodosLosParametrosAlService() throws Exception {
    UUID subcategoriaId = UUID.randomUUID();
    UUID categoriaId = UUID.randomUUID();
    UUID donanteId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID necesidadId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    FiltroDonacionesIndependientes esperado =
        new FiltroDonacionesIndependientes(
            List.of(TipoEstadoDonacion.EN_DEPOSITO, TipoEstadoDonacion.ENTREGADA),
            subcategoriaId,
            categoriaId,
            donanteId,
            entidadId,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 5),
            necesidadId,
            id);
    ParametrosListado listado = new ParametrosListado(3, 25, "campera", "ANTIGUAS");
    when(donacionesIndependientesService.obtenerConFiltros(esperado, listado))
        .thenReturn(new Pagina<>(List.of(), 0));

    mockMvc
        .perform(
            get(RUTA)
                .param("estado", "EN_DEPOSITO")
                .param("estado", "ENTREGADA")
                .param("page", "3")
                .param("size", "25")
                .param("q", "campera")
                .param("orden", "ANTIGUAS")
                .param("subcategoriaId", subcategoriaId.toString())
                .param("categoriaId", categoriaId.toString())
                .param("donanteId", donanteId.toString())
                .param("entidadId", entidadId.toString())
                .param("desde", "2026-11-01")
                .param("hasta", "2026-11-05")
                .param("necesidadId", necesidadId.toString())
                .param("id", id.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(0));

    verify(donacionesIndependientesService).obtenerConFiltros(esperado, listado);
  }

  @Test
  void obtenerTodas_conFechaMalFormada_retorna400() throws Exception {
    mockMvc
        .perform(get(RUTA).param("desde", "01/11/2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCatalog.ARGUMENTO_INVALIDO.getCode()));

    verify(donacionesIndependientesService, never()).obtenerConFiltros(any(), any());
  }

  @Test
  void obtenerTodas_conEstadoInexistente_sigueRespondiendo400() throws Exception {
    mockMvc
        .perform(get(RUTA).param("estado", "INEXISTENTE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCatalog.ARGUMENTO_INVALIDO.getCode()));
  }

  @Test
  void obtenerTodas_conDesdePosteriorAHasta_retorna400() throws Exception {
    when(donacionesIndependientesService.obtenerConFiltros(any(), any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get(RUTA).param("desde", "2026-11-05").param("hasta", "2026-11-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCatalog.ARGUMENTO_INVALIDO.getCode()));
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
            5,
            null);

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

  // ===================== R11-43: conteos de la organización =====================

  @Test
  void conteos_devuelveLasCifrasDeLaOrganizacion() throws Exception {
    UUID entidad = UUID.randomUUID();
    UUID necesidad = UUID.randomUUID();
    when(donacionesIndependientesService.conteos(entidad, null))
        .thenReturn(
            new ConteosDeEntidadDTO(
                List.of(new PorEstado("EN_PUERTA", 2)),
                new EntregadasEnElMes("2026-11", 5),
                List.of(new PorNecesidad(necesidad, List.of(new PorEstado("EN_PUERTA", 1))))));

    mockMvc
        .perform(get(RUTA + "/conteos").param("entidadId", entidad.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.porEstado[0].estado").value("EN_PUERTA"))
        .andExpect(jsonPath("$.porEstado[0].cantidad").value(2))
        .andExpect(jsonPath("$.entregadasEnElMes.mes").value("2026-11"))
        .andExpect(jsonPath("$.entregadasEnElMes.cantidad").value(5))
        .andExpect(jsonPath("$.porNecesidad[0].necesidadId").value(necesidad.toString()))
        .andExpect(jsonPath("$.porNecesidad[0].porEstado[0].cantidad").value(1));
  }

  @Test
  void conteos_conNecesidad_laPasaAlService() throws Exception {
    UUID entidad = UUID.randomUUID();
    UUID necesidad = UUID.randomUUID();
    when(donacionesIndependientesService.conteos(entidad, necesidad))
        .thenReturn(
            new ConteosDeEntidadDTO(List.of(), new EntregadasEnElMes("2026-11", 0), List.of()));

    mockMvc
        .perform(
            get(RUTA + "/conteos")
                .param("entidadId", entidad.toString())
                .param("necesidadId", necesidad.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entregadasEnElMes.cantidad").value(0));
  }

  @Test
  void conteos_sinEntidad_retorna400() throws Exception {
    mockMvc.perform(get(RUTA + "/conteos")).andExpect(status().isBadRequest());
  }
}
