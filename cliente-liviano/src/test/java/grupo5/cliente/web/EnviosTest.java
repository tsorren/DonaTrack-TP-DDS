package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import grupo5.cliente.maqueta.MaquetaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * La pieza común de los envíos (SPEC-08), de punta a punta en el servidor: el POST redirige y deja
 * el aviso, y la pantalla de destino lo muestra en la región #avisos de su layout, el público y el
 * privado. Sin aviso, la región queda vacía, como antes.
 */
@WebMvcTest(PantallasController.class)
@Import({MaquetaConfig.class, Pantallas.class, EnviosTest.UnEnvio.class})
@ActiveProfiles("maqueta")
class EnviosTest {

  private static final String TEXTO = "Listo. Guardamos tus datos.";
  private static final String AVISO =
      "<p class=\"aviso-breve\" data-estado=\"hecho\">"
          + TEXTO
          + "<button\n    type=\"button\" class=\"aviso-cerrar\" data-cerrar aria-label=\"Cerrar el"
          + " aviso\">×</button></p>";

  /** Un envío de prueba que guarda y vuelve a la pantalla que se le pide. */
  @Controller
  static class UnEnvio {

    @PostMapping("/prueba-envio/{destino}")
    String guardar(@PathVariable String destino, RedirectAttributes atributos) {
      return Envios.listo(destino, TEXTO, atributos);
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void elAvisoLlegaALaRegionDeAvisosDeCadaLayout() throws Exception {
    for (String destino : new String[] {"publico-legales", "donante-seguimiento-prueba"}) {
      MvcResult envio =
          mvc.perform(post("/prueba-envio/" + destino))
              .andExpect(redirectedUrl("/" + destino))
              .andExpect(flash().attribute(Envios.AVISO, TEXTO))
              .andReturn();

      mvc.perform(get("/" + destino).flashAttrs(envio.getFlashMap()))
          .andExpect(
              content().string(containsString("x-on:focusout=\"seguir\">" + AVISO + "</div>")));
    }
  }

  @Test
  void sinEnvioLaRegionQuedaVacia() throws Exception {
    mvc.perform(get("/publico-legales"))
        .andExpect(content().string(containsString("x-on:focusout=\"seguir\"></div>")))
        .andExpect(content().string(not(containsString("aviso-breve"))));
  }
}
