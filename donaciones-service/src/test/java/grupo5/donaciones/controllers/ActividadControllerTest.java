package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.actividad.ActividadDTO;
import grupo5.donaciones.dto.actividad.AnotacionActividadDTO;
import grupo5.donaciones.dto.actividad.FiltroActividad;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.services.ParametrosListado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** R11-26: GET y POST /api/actividad (C4 a C6 de la spec R11-p2-actividad). */
class ActividadControllerTest extends AbstractDonacionesWebMvcTest {

  private static final String RUTA = "/api/actividad";

  private static ActividadDTO fila() {
    return new ActividadDTO(
        UUID.randomUUID(),
        LocalDateTime.of(2026, 11, 12, 18, 20),
        "admin-a",
        AccionDeAdministracion.PROPUESTA_APROBADA,
        UUID.randomUUID().toString(),
        "Comedor Barrio Ejemplo");
  }

  @Test
  void listar_sinParametros_devuelveLaPaginaYElTotal() throws Exception {
    when(actividadService.listar(any(), any())).thenReturn(new Pagina<>(List.of(fila()), 25));

    mockMvc
        .perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "25"))
        .andExpect(jsonPath("$[0].autor").value("admin-a"))
        .andExpect(jsonPath("$[0].accion").value("PROPUESTA_APROBADA"))
        .andExpect(jsonPath("$[0].detalle").value("Comedor Barrio Ejemplo"));

    verify(actividadService)
        .listar(FiltroActividad.ninguno(), new ParametrosListado(null, null, null, null));
  }

  @Test
  void listar_conFiltros_losPasaAlServicio() throws Exception {
    when(actividadService.listar(any(), any())).thenReturn(new Pagina<>(List.of(), 3));

    mockMvc
        .perform(
            get(RUTA)
                .param("autor", "admin-a", "admin-b")
                .param("accion", "DONACION_VENCIDA")
                .param("desde", "2026-11-10")
                .param("hasta", "2026-11-12")
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "3"));

    verify(actividadService)
        .listar(
            new FiltroActividad(
                List.of("admin-a", "admin-b"),
                List.of("DONACION_VENCIDA"),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12)),
            new ParametrosListado(0, 1, null, null));
  }

  @Test
  void listar_conUnaFechaInvalida_responde400SinConsultar() throws Exception {
    mockMvc
        .perform(get(RUTA).param("desde", "2026-13-40"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));

    verify(actividadService, never()).listar(any(), any());
  }

  @Test
  void listar_conUnParametroQueElServicioRechaza_responde400() throws Exception {
    when(actividadService.listar(any(), any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get(RUTA).param("accion", "INVENTADA"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void anotar_responde201ConLaFila() throws Exception {
    ActividadDTO anotada =
        new ActividadDTO(
            UUID.randomUUID(),
            LocalDateTime.of(2026, 11, 12, 11, 10),
            "admin-a",
            AccionDeAdministracion.CAMION_AGREGADO,
            "camion-1",
            "AB123CD");
    when(actividadService.anotar(any())).thenReturn(anotada);

    mockMvc
        .perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accion":"CAMION_AGREGADO","objetoId":"camion-1","detalle":"AB123CD",
                     "autor":"otra-cuenta"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.autor").value("admin-a"))
        .andExpect(jsonPath("$.accion").value("CAMION_AGREGADO"));

    // El autor del cuerpo no llega al servicio: el DTO no lo tiene.
    verify(actividadService)
        .anotar(eq(new AnotacionActividadDTO("CAMION_AGREGADO", "camion-1", "AB123CD")));
  }

  @Test
  void anotar_unaAccionDeDonaciones_responde400() throws Exception {
    when(actividadService.anotar(any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accion\":\"PROPUESTA_APROBADA\",\"objetoId\":\"p-1\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }
}
