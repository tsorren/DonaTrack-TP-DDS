package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CampanitaPuerto;
import grupo5.cliente.vistas.CampanitaVista;
import grupo5.cliente.vistas.CampanitaVista.Fila;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El desplegable de la campanita y abrir un aviso (R11-29, R11-30), con el puerto de mentira. */
@WebMvcTest(CampanitaController.class)
@ActiveProfiles("maqueta")
@Import(MaquetaConfig.class)
class CampanitaControllerTest {

  private static final String AHORA = "2026-10-15T16:25";

  @MockitoBean private CampanitaPuerto campanita;
  @Autowired private MockMvc mvc;

  private static CampanitaVista dos(long sinLeer) {
    return new CampanitaVista(
        List.of(
            new Fila("d1", "Donación en camino", "2026-10-15T16:20"),
            new Fila("d2", "Donación asignada", "2026-10-15T13:25")),
        sinLeer,
        AHORA);
  }

  @Test
  void conHtmxTraeLasFilasConSuFormularioYHaceCuanto() throws Exception {
    when(campanita.recientes(eq("donante"), any())).thenReturn(dos(2));

    mvc.perform(get("/donante-notificaciones-recientes").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("2 sin leer")))
        .andExpect(content().string(containsString("Donación en camino")))
        .andExpect(content().string(containsString("action=\"/donante-notificaciones-abrir\"")))
        .andExpect(content().string(containsString("value=\"d1\"")))
        .andExpect(content().string(containsString("hace 5 minutos")))
        .andExpect(content().string(not(containsString("<html"))));
  }

  /** C9: si hay más sin leer que filas, la cifra dice cuántas de cuántas se ven. */
  @Test
  void conMasSinLeerQueFilasDiceCuantasSeVen() throws Exception {
    when(campanita.recientes(eq("entidad"), any())).thenReturn(dos(1234));

    mvc.perform(get("/entidad-notificaciones-recientes").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("2 de 1.234 sin leer")))
        .andExpect(content().string(containsString("action=\"/entidad-notificaciones-abrir\"")));
  }

  @Test
  void sinSinLeerLoDiceYNoHayFilas() throws Exception {
    when(campanita.recientes(eq("admin"), any()))
        .thenReturn(new CampanitaVista(List.of(), 0, AHORA));

    mvc.perform(get("/admin-notificaciones-recientes").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("No hay notificaciones sin leer para la administración.")))
        .andExpect(content().string(not(containsString("campanita-filas"))));
  }

  /** C11: sin respuesta de Notificaciones, el texto de error con 200, sin romper la pantalla. */
  @Test
  void sinRespuestaDiceQueNoPudoConDoscientos() throws Exception {
    when(campanita.recientes(eq("donante"), any())).thenThrow(new IllegalStateException("caído"));

    mvc.perform(get("/donante-notificaciones-recientes").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No pudimos traer tus notificaciones.")));
  }

  /** Sin HTMX (se abrió la dirección a mano), la bandeja en «Sin leer». */
  @Test
  void sinHtmxVaALaBandejaSinLeer() throws Exception {
    mvc.perform(get("/donante-notificaciones-recientes"))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl("/donante-notificaciones?ver=sin-leer"));
  }

  /** C12: el destino lo da el puerto; un destino que mande el navegador no se usa. */
  @Test
  void abrirLlevaAlDestinoDelPuertoYNoAlDelNavegador() throws Exception {
    when(campanita.abrir(eq("donante"), eq("d1"), any()))
        .thenReturn("/donante-donaciones-detalle?donacion=x1");

    mvc.perform(
            post("/donante-notificaciones-abrir")
                .param("id", "d1")
                .param("destino", "https://otro.sitio")
                .with(csrf()))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl("/donante-donaciones-detalle?donacion=x1"));
    verify(campanita).abrir(eq("donante"), eq("d1"), any());
  }
}
