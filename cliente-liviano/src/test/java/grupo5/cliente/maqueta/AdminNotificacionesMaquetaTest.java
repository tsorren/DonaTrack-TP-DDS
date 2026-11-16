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
        .andExpect(content().string(containsString("Listo: marcaste todas como leídas.")));
  }

  @Test
  void losEscenariosDeLaMaquetaSiguenAndando() throws Exception {
    mvc.perform(get("/admin-notificaciones").param("escenario", "error"))
        .andExpect(
            content()
                .string(
                    containsString("No pudimos cargar las notificaciones de la administración.")))
        .andExpect(content().string(not(containsString("Mostrando"))));
    mvc.perform(get("/admin-notificaciones").param("escenario", "vacio"))
        .andExpect(
            content()
                .string(containsString("Todavía no hay notificaciones para la administración.")));
  }
}
