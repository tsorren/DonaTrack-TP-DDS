package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminAsignacionesVista.Cuentas;
import grupo5.cliente.vistas.ConsultaAsignaciones;
import grupo5.cliente.vistas.Tramo;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Asignar donaciones fuera de la maqueta: la lista sin asignaciones corridas, los envíos que
 * guardan y vuelven con el aviso, el 409 de una aprobación y las pantallas de resultado de la
 * maqueta, que acá no existen.
 */
@WebMvcTest({AdminAsignacionesController.class, AdminAsignacionesEnvios.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminCamionesEnviosTest.Cuenta.class})
class AdminAsignacionesEnviosTest {

  private static final String ID = "6f1c2c1e-0000-4000-8000-0000000000b1";

  @MockitoBean private AdminOperacionPuerto operacion;
  @Autowired private MockMvc mvc;

  @Test
  void sinAsignacionesCorridasLoDiceSinInventarUnaFecha() throws Exception {
    when(operacion.asignaciones(any(), eq("")))
        .thenReturn(
            new AdminAsignacionesVista(
                ConsultaAsignaciones.primera(),
                new Cuentas(0, 0, 0),
                null,
                new Tramo<>(List.of(), 0, 1, 1),
                List.of(),
                null,
                List.of(),
                null,
                null));

    mvc.perform(get("/admin-asignaciones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no se corrió ninguna asignación.")))
        .andExpect(content().string(containsString("No hay propuestas que esperen tu decisión.")))
        .andExpect(content().string(containsString("aria-label=\"Qué querés revisar\"")));
  }

  @Test
  void laPestanaElFiltroYLaPaginaVanDeLaDireccionAlPuerto() throws Exception {
    when(operacion.asignaciones(any(), eq("")))
        .thenReturn(
            new AdminAsignacionesVista(
                new ConsultaAsignaciones("conflictos", "", "", "", 2),
                new Cuentas(30, 5, 25),
                "hoy, 0:00 · generó 12 propuestas",
                new Tramo<>(List.of(), 25, 2, 3),
                List.of(),
                null,
                List.of(),
                null,
                null));

    mvc.perform(get("/admin-asignaciones?pestana=conflictos&algoritmo=las-dos&pagina=2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Propuestas en conflicto · 25")));
    verify(operacion)
        .asignaciones(new ConsultaAsignaciones("conflictos", "las-dos", "", "", 2), "");
  }

  @Test
  void correrAprobarYDescartarVuelvenALaPestanaConElAviso() throws Exception {
    when(operacion.correrAsignacion()).thenReturn(2);
    when(operacion.decidirPropuesta(ID, true, null)).thenReturn(1);
    when(operacion.decidirPropuesta(ID, false, "Ya no hace falta")).thenReturn(0);

    mvc.perform(post("/admin-asignaciones-corrida"))
        .andExpect(redirectedUrl("/admin-asignaciones"))
        .andExpect(flash().attribute(Envios.AVISO, "La asignación generó 2 propuestas nuevas."));
    mvc.perform(
            post("/admin-asignaciones-aprobada")
                .param("propuesta", ID)
                .param("organizacion", "Comedor Ejemplo")
                .param("pestana", "conflictos"))
        .andExpect(redirectedUrl("/admin-asignaciones?pestana=conflictos"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Aprobaste la propuesta para Comedor Ejemplo. Las donaciones quedaron asignadas"
                        + " y salen del depósito cuando se arme la ruta. 1 propuesta que pedía la"
                        + " misma donación pasó a Conflictos."));
    mvc.perform(
            post("/admin-asignaciones-descartada")
                .param("propuesta", ID)
                .param("organizacion", "Comedor Ejemplo")
                .param("pestana", "otra")
                .param("motivo", "Ya no hace falta"))
        .andExpect(redirectedUrl("/admin-asignaciones"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Descartaste la propuesta para Comedor Ejemplo. Las donaciones siguen en el"
                        + " depósito para otra asignación."));
  }

  @Test
  void unaAprobacionRechazadaDiceElMotivo() throws Exception {
    when(operacion.decidirPropuesta(ID, true, null))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.CONFLICT,
                "Conflict",
                HttpHeaders.EMPTY,
                "{\"code\": \"ERR-EST-521\", \"type\": \"BusinessStateException\"}"
                    .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8));

    mvc.perform(
            post("/admin-asignaciones-aprobada")
                .param("propuesta", ID)
                .param("organizacion", "Merendero Ejemplo"))
        .andExpect(redirectedUrl("/admin-asignaciones"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "No se pudo aprobar: la necesidad de Merendero Ejemplo ya no está activa (la"
                        + " organización la dio de baja o ya se cumplió). No se asignó nada; podés"
                        + " descartar la propuesta."));
  }

  @Test
  void lasPantallasDeResultadoDeLaMaquetaNoExisten() throws Exception {
    mvc.perform(get("/admin-asignaciones-corrida")).andExpect(status().isNotFound());
  }
}
