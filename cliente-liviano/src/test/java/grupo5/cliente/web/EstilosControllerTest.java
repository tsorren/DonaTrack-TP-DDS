package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EstilosController.class)
class EstilosControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void catalogoUsaElLayoutAccesible() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andExpect(view().name("paginas/estilos"))
        .andExpect(content().string(containsString("<html lang=\"es-AR\" data-theme=\"light\">")))
        .andExpect(content().string(containsString("href=\"#contenido\"")))
        .andExpect(content().string(containsString("<main id=\"contenido\"")))
        .andExpect(content().string(containsString("aria-live=\"polite\"")))
        // webjars-locator-lite agrega la versión del pom.xml a la URL.
        .andExpect(content().string(containsString("/webjars/htmx.org/")))
        .andExpect(content().string(containsString("/webjars/picocss__pico/")));
  }

  @Test
  void catalogoMuestraLosEstadosDeCadaComponente() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("aria-current=\"page\"")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")))
        .andExpect(content().string(containsString("aria-describedby=\"remito-ayuda\"")))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("Código de seguimiento: a1b2c3d4e5f6")))
        .andExpect(content().string(containsString("insignia insignia--con-problema")))
        .andExpect(content().string(containsString("htmx-indicator htmx-request")))
        .andExpect(content().string(containsString("<dialog id=\"dialogo-demo\"")))
        .andExpect(content().string(containsString("class=\"tarjeta-portada\"")));
  }

  @Test
  void muestraDelArmazonUsaElLayoutPrivado() throws Exception {
    mvc.perform(get("/estilos/armazon"))
        .andExpect(status().isOk())
        .andExpect(view().name("paginas/estilos-armazon"))
        .andExpect(content().string(containsString("<body class=\"armazon\">")))
        .andExpect(content().string(containsString("href=\"#contenido\"")))
        .andExpect(content().string(containsString("<main id=\"contenido\"")))
        .andExpect(content().string(containsString("aria-live=\"polite\"")))
        .andExpect(content().string(containsString("aria-current=\"page\"")))
        .andExpect(content().string(containsString("aria-label=\"Cuenta de Ana\"")))
        .andExpect(content().string(containsString("aria-label=\"Notificaciones\"")))
        .andExpect(content().string(containsString("Hola, Ana")));
  }
}
