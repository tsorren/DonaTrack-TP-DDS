package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El armazón de la huerta en /estilos (C1-07 a C1-10): las migas de pan, componente nuevo, con
 * enlaces reales y la página actual sin enlace, y el pie de todas las páginas con su muestra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosArmazonTest {

  @Autowired private MockMvc mvc;

  @Test
  void elCatalogoMuestraLasMigasConLaPaginaActualSinEnlace() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("<nav class=\"migas\" aria-label=\"Dónde estás")))
        .andExpect(
            content().string(containsString("href=\"/donante-donaciones\">Mis donaciones</a>")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<li aria-current=\"page\">Lentejas secas, 15 kg</li>.*")));
  }

  @Test
  void elCatalogoMuestraElPieConSusEnlaces() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(content().string(containsString("<code>.raices-pie</code>")))
        .andExpect(
            content().string(matchesPattern("(?s).*class=\"raices-pie\".*class=\"raices-pie\".*")));
  }
}
