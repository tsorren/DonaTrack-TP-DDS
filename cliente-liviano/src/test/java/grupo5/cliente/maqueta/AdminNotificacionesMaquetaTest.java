package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.web.AdminNotificacionesController;
import grupo5.cliente.web.Pantallas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La bandeja de la administración en la maqueta (R11-30): los ejemplos vienen del adaptador de la
 * maqueta, no de la plantilla, con las categorías del admin y sin el nombre de ningún archivo.
 */
@WebMvcTest(controllers = AdminNotificacionesController.class)
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, AdminNotificacionesMaqueta.class})
class AdminNotificacionesMaquetaTest {

  @Autowired private MockMvc mvc;

  @Test
  void muestraLosEjemplosDeLaMaquetaConLasCategoriasDelAdmin() throws Exception {
    mvc.perform(get("/admin-notificaciones"))
        .andExpect(status().isOk())
        // El título es «Novedades» en los tres roles (ficha guia-org-buzon, sección 6).
        .andExpect(content().string(containsString("<h1>Novedades</h1>")))
        .andExpect(content().string(containsString("<title>Novedades - DonaTrack</title>")))
        .andExpect(content().string(containsString("3 sin leer. Al 15 de octubre de 2026, 18:45.")))
        .andExpect(content().string(containsString("Terminó una importación")))
        .andExpect(content().string(containsString("Nuevo mensaje de contacto Nº 12")))
        .andExpect(content().string(containsString("Ver qué pasó")))
        .andExpect(content().string(containsString("Mensajes de contacto")))
        .andExpect(content().string(not(containsString(".csv"))))
        .andExpect(content().string(not(containsString("Problemas de logística"))));
  }

  @Test
  void filtraPorCategoriaYMarcarDejaElAviso() throws Exception {
    mvc.perform(get("/admin-notificaciones").param("tema", "contacto"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Coinciden 2 notificaciones.")))
        .andExpect(content().string(not(containsString("Una importación se cortó"))));
    mvc.perform(get("/admin-notificaciones-leidas"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Listo: marcaste todas como leídas.")))
        // ADM2-09: el aviso breve con signo del galpón (admin-donaciones :: hecho), con su cierre.
        .andExpect(content().string(containsString("data-signo=\"e-confirmada\"")))
        .andExpect(content().string(containsString("aria-label=\"Cerrar el aviso\"")));
    mvc.perform(get("/admin-notificaciones-leidas").param("marcadas", "a1"))
        .andExpect(content().string(containsString("Listo: marcaste la notificación como leída.")))
        .andExpect(content().string(containsString("data-signo=\"e-confirmada\"")));
    mvc.perform(get("/admin-notificaciones-leidas").param("marcadas", "").param("noLeidas", "a4"))
        .andExpect(
            content().string(containsString("Listo: marcaste la notificación como no leída.")))
        .andExpect(content().string(containsString("data-signo=\"e-confirmada\"")));
  }

  @Test
  void laPantallaVaEnElGalponConLaCabeceraYLaBajada() throws Exception {
    // ADM2-09: la pila del galpón con su data-pantalla, sin la mesa heredada ni el rótulo de
    // arriba.
    mvc.perform(get("/admin-notificaciones"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "<div class=\"pila\" data-espacio=\"4\" data-pantalla=\"novedades\">")))
        .andExpect(
            content()
                .string(containsString("<header class=\"racimo\" data-alinea=\"entre arriba\">")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Son los mismos para todas las cuentas de administración: si alguien")))
        .andExpect(content().string(not(containsString("class=\"mesa"))))
        .andExpect(content().string(not(containsString("class=\"bajada\""))))
        .andExpect(content().string(not(containsString("<p class=\"rotulo\">Administración</p>"))));
  }

  @Test
  void losEscenariosDeLaMaquetaSiguenAndando() throws Exception {
    mvc.perform(get("/admin-notificaciones").param("escenario", "error"))
        .andExpect(
            content()
                .string(
                    containsString("No pudimos cargar las notificaciones de la administración.")))
        .andExpect(content().string(not(containsString("Mostrando"))))
        // El error de carga es el aviso en la página del catálogo, no el banner.
        .andExpect(content().string(containsString("class=\"aviso\" data-tipo=\"error\"")))
        .andExpect(content().string(containsString("data-signo=\"e-problema\"")))
        .andExpect(content().string(containsString("escribinos")))
        .andExpect(content().string(not(containsString("class=\"banner\""))));
    mvc.perform(get("/admin-notificaciones").param("escenario", "lento"))
        .andExpect(content().string(containsString("class=\"cargando\"")))
        .andExpect(content().string(containsString("aria-busy=\"true\"")))
        // La lista que viene, como en la organización y en quien dona: tres renglones.
        .andExpect(
            content()
                .string(
                    containsString(
                        "<div class=\"esqueleto\" aria-hidden=\"true\"><i></i><i></i><i></i></div>")))
        .andExpect(content().string(not(containsString("Mostrando"))));
    mvc.perform(get("/admin-notificaciones").param("escenario", "vacio"))
        .andExpect(
            content()
                .string(containsString("Todavía no hay notificaciones para la administración.")))
        .andExpect(content().string(containsString("class=\"vacio\"")));
  }
}
