package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.OrigenEntidad;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.services.ResultadoRegistro;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * R11-26 (C1, C2, C3 y C7 de la spec R11-p2-actividad): cada acción de Donaciones avisa al registro
 * después de salir bien, con lo que tocó. Si la acción falla, no avisa. Quién la hizo y si queda
 * fila (solo con un token de ADMIN) lo decide el servicio del registro (ActividadServiceTest).
 */
class RegistroDeActividadEnLasAccionesTest extends AbstractDonacionesWebMvcTest {

  private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

  @Test
  void aprobarUnaPropuesta_laRegistra() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(
            put("/api/asignaciones/propuestas/" + id + "/estado")
                .contentType(JSON)
                .content("{\"estado\":\"APROBADA\"}"))
        .andExpect(status().isOk());

    verify(actividadService).propuestaDecidida(id, EstadoPropuesta.APROBADA);
  }

  @Test
  void unaAprobacionQueFalla_noDejaRegistro() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new BusinessStateException(ErrorCatalog.NECESIDAD_INACTIVA))
        .when(propuestaDeAsignacionService)
        .actualizarEstado(eq(id), any(), any());

    mockMvc
        .perform(
            put("/api/asignaciones/propuestas/" + id + "/estado")
                .contentType(JSON)
                .content("{\"estado\":\"APROBADA\"}"))
        .andExpect(status().isConflict());

    verifyNoInteractions(actividadService);
  }

  @Test
  void correrLaAsignacion_registraCuantasPropuestasNuevasHubo() throws Exception {
    PropuestaDTO propuesta = mock(PropuestaDTO.class);
    when(propuestaDeAsignacionService.ejecutarAsignacion())
        .thenReturn(List.of(propuesta, propuesta, propuesta));

    mockMvc.perform(post("/api/asignaciones/ejecuciones")).andExpect(status().isCreated());

    verify(actividadService).registrar(AccionDeAdministracion.ASIGNACION_CORRIDA, null, "3");
  }

  @Test
  void marcarVencida_registraLaDonacionAunqueXActorDigaOtraCosa() throws Exception {
    UUID id = UUID.randomUUID();
    when(donacionesIndependientesService.cambiarEstado(eq(id), any(), eq("otra-cuenta")))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", "otra-cuenta")
                .contentType(JSON)
                .content("{\"estado\":\"VENCIDA\",\"justificacion\":\"Se venció\"}"))
        .andExpect(status().isOk());

    // El registro no recibe X-Actor: el autor lo pone el servicio desde el token.
    verify(actividadService)
        .registrar(AccionDeAdministracion.DONACION_VENCIDA, id.toString(), null);
  }

  @Test
  void otroCambioDeEstado_noEsUnaAccionDelRegistro() throws Exception {
    UUID id = UUID.randomUUID();
    when(donacionesIndependientesService.cambiarEstado(eq(id), any(), any()))
        .thenReturn(mock(DonacionIndependienteResponseDTO.class));

    mockMvc
        .perform(
            patch("/donaciones-independientes/" + id + "/estado")
                .header("X-Actor", "SISTEMA")
                .contentType(JSON)
                .content("{\"estado\":\"ASIGNACION_REALIZADA\"}"))
        .andExpect(status().isOk());

    verifyNoInteractions(actividadService);
  }

  @Test
  void registrarUnDonanteNuevo_loRegistra() throws Exception {
    DonanteOutputDTO donante = mock(DonanteOutputDTO.class);
    when(donantesService.crearDonante(any(DonanteInputDTO.class)))
        .thenReturn(new ResultadoRegistro<>(donante, true));

    mockMvc
        .perform(
            post("/api/donantes")
                .contentType(JSON)
                .content("{\"idPersona\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isCreated());

    verify(actividadService).donanteRegistrado(donante);
  }

  @Test
  void unDonanteQueYaEra_noSeRegistraDeNuevo() throws Exception {
    when(donantesService.crearDonante(any(DonanteInputDTO.class)))
        .thenReturn(new ResultadoRegistro<>(mock(DonanteOutputDTO.class), false));

    mockMvc
        .perform(
            post("/api/donantes")
                .contentType(JSON)
                .content("{\"idPersona\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isOk());

    verify(actividadService, never()).donanteRegistrado(any());
  }

  @Test
  void subirUnArchivo_registraLaImportacionConSuNombre() throws Exception {
    UUID archivoId = UUID.randomUUID();
    ArchivoOutputDTO subido =
        new ArchivoOutputDTO(
            archivoId,
            "clave.csv",
            "PENDIENTE",
            null,
            0,
            0,
            0,
            0,
            List.of(),
            "donantes-noviembre.csv",
            null);
    when(archivoDonantesService.cargarArchivoDonantes(any(ArchivoDonantesSubido.class)))
        .thenReturn(subido);

    mockMvc
        .perform(
            multipart("/api/donantes/archivos")
                .file(
                    new MockMultipartFile(
                        "archivo", "donantes-noviembre.csv", "text/csv", "a;b\n".getBytes())))
        .andExpect(status().isAccepted());

    verify(actividadService)
        .registrar(
            AccionDeAdministracion.IMPORTACION_SUBIDA,
            archivoId.toString(),
            "donantes-noviembre.csv");
  }

  @Test
  void aprobarUnaSolicitud_registraLaOrganizacionAprobada() throws Exception {
    EntidadBeneficiariaOutputDTO entidad = mock(EntidadBeneficiariaOutputDTO.class);
    when(entidadBeneficiariaService.crearEntidad(any()))
        .thenReturn(new ResultadoRegistro<>(entidad, true));

    mockMvc
        .perform(
            post("/api/entidades")
                .contentType(JSON)
                .content("{\"juridicaId\":\"" + UUID.randomUUID() + "\",\"origen\":\"SOLICITUD\"}"))
        .andExpect(status().isCreated());

    verify(actividadService).organizacionCreada(entidad, OrigenEntidad.SOLICITUD);
  }

  @Test
  void registrarUnaOrganizacionSinOrigen_registraLaOrganizacionRegistrada() throws Exception {
    EntidadBeneficiariaOutputDTO entidad = mock(EntidadBeneficiariaOutputDTO.class);
    when(entidadBeneficiariaService.crearEntidad(any()))
        .thenReturn(new ResultadoRegistro<>(entidad, true));

    mockMvc
        .perform(
            post("/api/entidades")
                .contentType(JSON)
                .content("{\"juridicaId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isCreated());

    verify(actividadService).organizacionCreada(entidad, OrigenEntidad.ADMIN);
  }

  @Test
  void unOrigenQueNoExiste_responde400SinCrear() throws Exception {
    mockMvc
        .perform(
            post("/api/entidades")
                .contentType(JSON)
                .content("{\"juridicaId\":\"" + UUID.randomUUID() + "\",\"origen\":\"OTRO\"}"))
        .andExpect(status().isBadRequest());

    verify(entidadBeneficiariaService, never()).crearEntidad(any());
    verifyNoInteractions(actividadService);
  }
}
