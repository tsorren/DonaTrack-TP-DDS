package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** La pantalla de prueba está en src/test/resources/templates/prototipo/cozy-medieval/. */
@WebMvcTest(PrototipoController.class)
@ActiveProfiles("maqueta")
class PrototipoControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void unaPantallaDeNivelTresMarcaSuSeccionEnElMenuDeSuRol() throws Exception {
    mvc.perform(get("/prototipo/donante-donaciones-prueba"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/cozy-medieval/donante-donaciones-prueba"))
        .andExpect(model().attribute("alternativa", "cozy-medieval"))
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(content().string(containsString("<p>Ana</p>")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/prototipo/donante-donaciones\" aria-current=\"page\"")));
  }

  @Test
  void unaSeccionSinPantallaMuestraLaTierraSinExplorar() throws Exception {
    mvc.perform(get("/prototipo/admin-ranking"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/cozy-medieval/sin-dibujar"))
        .andExpect(model().attribute("seccion", "Ranking"))
        .andExpect(content().string(containsString("todavía no está dibujada")))
        .andExpect(content().string(containsString("href=\"/prototipo/admin-inicio\"")));
  }

  @Test
  void loQueNoEstaNiEnElMenuNiDibujadoNoExiste() throws Exception {
    mvc.perform(get("/prototipo/donante-tesoro")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/..%2Fpaginas%2Festilos")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/sin-dibujar")).andExpect(status().isNotFound());
  }
}
