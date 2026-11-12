package grupo5.donaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.donantes.FilaConErrorDTO;
import grupo5.donaciones.dto.donantes.FiltroArchivos;
import grupo5.donaciones.dto.donantes.FiltroDonantes;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.dto.publico.EntidadParaDonanteDTO;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.ResultadoRegistro;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class DonantesControllerTest extends AbstractDonacionesWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void crearDonante_DeberiaRetornarCreatedYDto() throws Exception {
    UUID personaId = UUID.randomUUID();
    UUID donanteId = UUID.randomUUID();
    DonanteInputDTO input = new DonanteInputDTO(personaId);
    HumanaOutputDTO personaOutput =
        new HumanaOutputDTO(
            TipoPersona.HUMANA,
            personaId,
            TipoDocumento.DNI,
            "12345678",
            null,
            List.of(),
            "Juan",
            "Pérez",
            Genero.HOMBRE,
            LocalDate.of(1990, Month.JANUARY, 1));
    DonanteOutputDTO output = new DonanteOutputDTO(donanteId, personaOutput, true);

    when(donantesService.crearDonante(any(DonanteInputDTO.class)))
        .thenReturn(new ResultadoRegistro<>(output, true));

    mockMvc
        .perform(
            post("/api/donantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.idDonante").value(donanteId.toString()));
  }

  @Test
  void crearDonante_cuandoLaPersonaYaEraDonante_DeberiaRetornarOkYDto() throws Exception {
    UUID personaId = UUID.randomUUID();
    DonanteInputDTO input = new DonanteInputDTO(personaId);
    DonanteOutputDTO output = new DonanteOutputDTO(personaId, null, true);

    when(donantesService.crearDonante(any(DonanteInputDTO.class)))
        .thenReturn(new ResultadoRegistro<>(output, false));

    mockMvc
        .perform(
            post("/api/donantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idDonante").value(personaId.toString()));
  }

  @Test
  void crearDonante_conIdPersonaNulo_DeberiaRetornarBadRequest() throws Exception {
    DonanteInputDTO input = new DonanteInputDTO(null);

    mockMvc
        .perform(
            post("/api/donantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("idPersona"));
  }

  @Test
  void listarDonantes_DeberiaRetornarOkYLista() throws Exception {
    UUID donanteId = UUID.randomUUID();
    DonanteOutputDTO output = new DonanteOutputDTO(donanteId, null, true);

    when(donantesService.buscar(any(), any())).thenReturn(new Pagina<>(List.of(output), 31));

    mockMvc
        .perform(get("/api/donantes"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "31"))
        .andExpect(jsonPath("$.size()").value(1));

    verify(donantesService).buscar(FiltroDonantes.ninguno(), ParametrosListado.primeraPagina());
  }

  @Test
  void listarDonantes_pasaBusquedaFiltrosOrdenYPagina_R1101() throws Exception {
    UUID categoriaId = UUID.randomUUID();
    when(donantesService.buscar(any(), any())).thenReturn(new Pagina<>(List.of(), 0));

    mockMvc
        .perform(
            get("/api/donantes")
                .param("q", "gómez")
                .param("canal", "CORREO")
                .param("tipo", "HUMANA")
                .param("activo", "true")
                .param("categoriaId", categoriaId.toString())
                .param("orden", "NOMBRE")
                .param("page", "1")
                .param("size", "50"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "0"));

    verify(donantesService)
        .buscar(
            new FiltroDonantes("CORREO", TipoPersona.HUMANA, null, true, categoriaId),
            new ParametrosListado(1, 50, "gómez", "NOMBRE"));
  }

  @Test
  void listarDonantes_conTipoDesconocido_devuelve400SinConsultar() throws Exception {
    mockMvc
        .perform(get("/api/donantes").param("tipo", "MARCIANA"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));

    verifyNoInteractions(donantesService);
  }

  @Test
  void obtenerDonante_DeberiaRetornarOkYDto() throws Exception {
    UUID donanteId = UUID.randomUUID();
    DonanteOutputDTO output = new DonanteOutputDTO(donanteId, null, true);

    when(donantesService.obtenerPorId(donanteId)).thenReturn(output);

    mockMvc
        .perform(get("/api/donantes/{id}", donanteId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idDonante").value(donanteId.toString()));
  }

  @Test
  void eliminarDonante_DeberiaRetornarNoContent() throws Exception {
    UUID donanteId = UUID.randomUUID();

    mockMvc.perform(delete("/api/donantes/{id}", donanteId)).andExpect(status().isNoContent());
  }

  @Test
  void cargarArchivoDonantes_conArchivoMultipart_DeberiaRetornarAccepted() throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile(
            "archivo", "padron.csv", "text/csv", "TipoPersona,Nombre\nHumana,Ana\n".getBytes());
    ArchivoOutputDTO output =
        new ArchivoOutputDTO(
            UUID.randomUUID(), UUID.randomUUID() + ".csv", "PENDIENTE", null, 0, 0, 0, 0);

    when(archivoDonantesService.cargarArchivoDonantes(any(ArchivoDonantesSubido.class)))
        .thenReturn(output);

    mockMvc
        .perform(multipart("/api/donantes/archivos").file(archivo))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.totalFilas").doesNotExist())
        .andExpect(jsonPath("$.procesadas").value(0));
  }

  @Test
  void cargarArchivoDonantes_sinLaParteArchivo_DeberiaRetornarBadRequest() throws Exception {
    MockMultipartFile otraParte =
        new MockMultipartFile("otro", "padron.csv", "text/csv", "x".getBytes());

    mockMvc
        .perform(multipart("/api/donantes/archivos").file(otraParte))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
    verifyNoInteractions(archivoDonantesService);
  }

  @Test
  void cargarArchivoDonantes_conJsonConPath_DeberiaRetornarUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(
            post("/api/donantes/archivos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"path\":\"/etc/passwd\"}"))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(archivoDonantesService);
  }

  @Test
  void obtenerArchivoDonantes_DeberiaRetornarOkYDto() throws Exception {
    UUID archivoId = UUID.randomUUID();
    ArchivoOutputDTO output =
        new ArchivoOutputDTO(
            archivoId, "/ruta/donantes.csv", "PROCESADO_CON_ERRORES", 10, 10, 6, 3, 1);

    when(archivoDonantesService.obtenerPorId(archivoId)).thenReturn(output);

    mockMvc
        .perform(get("/api/donantes/archivos/{id}", archivoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(archivoId.toString()))
        .andExpect(jsonPath("$.estado").value("PROCESADO_CON_ERRORES"))
        .andExpect(jsonPath("$.totalFilas").value(10))
        .andExpect(jsonPath("$.procesadas").value(10))
        .andExpect(jsonPath("$.creadas").value(6))
        .andExpect(jsonPath("$.actualizadas").value(3))
        .andExpect(jsonPath("$.conError").value(1));
  }

  @Test
  void obtenerFilasConError_DeberiaRetornarLaPaginaConElTotalEnElHeader() throws Exception {
    UUID archivoId = UUID.randomUUID();
    when(archivoDonantesService.obtenerFilasConError(archivoId, 1, 2))
        .thenReturn(new Pagina<>(List.of(new FilaConErrorDTO(9, "FORMATO_INVALIDO", null)), 3));

    mockMvc
        .perform(
            get("/api/donantes/archivos/{id}/filas-con-error", archivoId)
                .param("page", "1")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "3"))
        .andExpect(jsonPath("$[0].linea").value(9))
        .andExpect(jsonPath("$[0].motivo").value("FORMATO_INVALIDO"));
  }

  @Test
  void obtenerFilasConError_deUnArchivoQueNoExiste_DeberiaRetornarNotFound() throws Exception {
    UUID archivoId = UUID.randomUUID();
    when(archivoDonantesService.obtenerFilasConError(archivoId, null, null))
        .thenThrow(new RecursoNoEncontradoException(archivoId));

    mockMvc
        .perform(get("/api/donantes/archivos/{id}/filas-con-error", archivoId))
        .andExpect(status().isNotFound());
  }

  @Test
  void obtenerFilasConError_conTamanioFueraDeRango_DeberiaRetornarBadRequest() throws Exception {
    UUID archivoId = UUID.randomUUID();
    when(archivoDonantesService.obtenerFilasConError(archivoId, null, 101))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get("/api/donantes/archivos/{id}/filas-con-error", archivoId).param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  // --- R11-20: la lista de importaciones ---------------------------------------------------------

  @Test
  void listarArchivosDonantes_devuelveLaPaginaConElTotalYLosCamposNuevos_C1() throws Exception {
    UUID archivoId = UUID.randomUUID();
    ArchivoOutputDTO output =
        new ArchivoOutputDTO(
            archivoId,
            archivoId + ".csv",
            "PROCESANDO",
            20000,
            8400,
            7950,
            410,
            40,
            List.of(),
            "donantes-octubre.csv",
            LocalDateTime.of(2026, 10, 15, 18, 2));
    when(archivoDonantesService.listar(
            new FiltroArchivos(
                List.of(EstadoArchivo.PENDIENTE, EstadoArchivo.PROCESANDO),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)),
            new ParametrosListado(0, 1, "octubre", "antiguas")))
        .thenReturn(new Pagina<>(List.of(output), 2));

    mockMvc
        .perform(
            get("/api/donantes/archivos")
                .param("estado", "PENDIENTE")
                .param("estado", "PROCESANDO")
                .param("desde", "2026-10-01")
                .param("hasta", "2026-10-31")
                .param("q", "octubre")
                .param("orden", "antiguas")
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "2"))
        .andExpect(jsonPath("$[0].id").value(archivoId.toString()))
        .andExpect(jsonPath("$[0].nombre").value("donantes-octubre.csv"))
        .andExpect(jsonPath("$[0].subidoEn").value("2026-10-15T18:02:00"))
        .andExpect(jsonPath("$[0].procesadas").value(8400));
  }

  @Test
  void listarArchivosDonantes_sinParametros_pideLaPrimeraPaginaPorDefecto() throws Exception {
    when(archivoDonantesService.listar(
            new FiltroArchivos(null, null, null), ParametrosListado.primeraPagina()))
        .thenReturn(new Pagina<>(List.of(), 0));

    mockMvc
        .perform(get("/api/donantes/archivos"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "0"))
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void listarArchivosDonantes_conEstadoQueNoExiste_DeberiaRetornarBadRequest_C6() throws Exception {
    mockMvc
        .perform(get("/api/donantes/archivos").param("estado", "OTRO"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
    verifyNoInteractions(archivoDonantesService);
  }

  @Test
  void listarArchivosDonantes_conFechaImposible_DeberiaRetornarBadRequest_C6() throws Exception {
    mockMvc
        .perform(get("/api/donantes/archivos").param("desde", "2026-13-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
    verifyNoInteractions(archivoDonantesService);
  }

  @Test
  void listarArchivosDonantes_conTamanioFueraDeRango_DeberiaRetornarBadRequest_C6()
      throws Exception {
    when(archivoDonantesService.listar(
            new FiltroArchivos(null, null, null), new ParametrosListado(null, 101, null, null)))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get("/api/donantes/archivos").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void cargarArchivoDonantes_respondeElNombreYLaFechaDeSubida_C7() throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile(
            "archivo", "donantes-octubre.csv", "text/csv", "TipoPersona\nHumana\n".getBytes());
    ArchivoOutputDTO output =
        new ArchivoOutputDTO(
            UUID.randomUUID(),
            UUID.randomUUID() + ".csv",
            "PENDIENTE",
            null,
            0,
            0,
            0,
            0,
            List.of(),
            "donantes-octubre.csv",
            LocalDateTime.of(2026, 10, 15, 18, 2));
    when(archivoDonantesService.cargarArchivoDonantes(any(ArchivoDonantesSubido.class)))
        .thenReturn(output);

    mockMvc
        .perform(multipart("/api/donantes/archivos").file(archivo))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.nombre").value("donantes-octubre.csv"))
        .andExpect(jsonPath("$.subidoEn").value("2026-10-15T18:02:00"));
  }

  // ===================== GET /api/donantes/{id}/entidades (R11-12) =====================

  @Test
  void listarEntidades_pasaLosFiltrosYDevuelveLaMarcaYElTotal() throws Exception {
    UUID donante = UUID.randomUUID();
    UUID categoria = UUID.randomUUID();
    UUID entidad = UUID.randomUUID();
    when(entidadBeneficiariaService.buscarParaDonante(donante, "comedor", categoria, true, 1, 6))
        .thenReturn(
            new Pagina<>(
                List.of(new EntidadParaDonanteDTO(entidad, "Comedor Sur", "Alimentos", true)), 7));

    mockMvc
        .perform(
            get("/api/donantes/" + donante + "/entidades")
                .param("nombre", "comedor")
                .param("categoriaId", categoria.toString())
                .param("soloDonadas", "true")
                .param("page", "1")
                .param("size", "6"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "7"))
        .andExpect(jsonPath("$[0].id").value(entidad.toString()))
        .andExpect(jsonPath("$[0].razonSocial").value("Comedor Sur"))
        .andExpect(jsonPath("$[0].rubro").value("Alimentos"))
        .andExpect(jsonPath("$[0].yaLeDono").value(true));
  }

  @Test
  void listarEntidades_sinSoloDonadas_lasPideTodas() throws Exception {
    UUID donante = UUID.randomUUID();
    when(entidadBeneficiariaService.buscarParaDonante(donante, null, null, false, null, null))
        .thenReturn(new Pagina<>(List.of(), 0));

    mockMvc
        .perform(get("/api/donantes/" + donante + "/entidades"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "0"));
  }

  @Test
  void listarEntidades_conUnaCategoriaMalEscrita_responde400() throws Exception {
    mockMvc
        .perform(get("/api/donantes/" + UUID.randomUUID() + "/entidades").param("categoriaId", "x"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
    verifyNoInteractions(entidadBeneficiariaService);
  }

  @Test
  void listarEntidades_conUnDonanteQueNoExiste_responde404() throws Exception {
    UUID donante = UUID.randomUUID();
    when(entidadBeneficiariaService.buscarParaDonante(donante, null, null, false, null, null))
        .thenThrow(new RecursoNoEncontradoException(donante));

    mockMvc
        .perform(get("/api/donantes/" + donante + "/entidades"))
        .andExpect(status().isNotFound());
  }
}
