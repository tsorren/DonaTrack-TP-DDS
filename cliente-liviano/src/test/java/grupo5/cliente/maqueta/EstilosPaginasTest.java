package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
 * El componente paginas (R11-11) en /estilos y en una lista del admin que usaba el paginador
 * provisorio: forma larga y corta en el mismo marcado, enlaces reales y nombres para el lector.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosPaginasTest {

  @Autowired private MockMvc mvc;

  @Test
  void elCatalogoMuestraElPaginadorConSusFormas() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"paginas\"")))
        .andExpect(content().string(containsString("class=\"paginas\"")))
        .andExpect(content().string(containsString("data-forma=\"corta\"")))
        .andExpect(content().string(containsString("aria-label=\"Primera página\"")))
        .andExpect(content().string(containsString("aria-label=\"Última página\"")))
        .andExpect(content().string(containsString("aria-label=\"Página 6\"")))
        .andExpect(content().string(containsString("aria-current=\"page\"")))
        .andExpect(content().string(containsString("Página 3 de 9")))
        .andExpect(content().string(containsString("href=\"/estilos?pagina=6#paginas\"")))
        .andExpect(content().string(containsString("paginas-hueco")));
  }

  @Test
  void laMuestraDelMedioSigueLaPaginaPedida() throws Exception {
    mvc.perform(get("/estilos?pagina=7"))
        .andExpect(content().string(containsString("Página 7 de 9")))
        .andExpect(content().string(containsString("href=\"/estilos?pagina=8#paginas\"")));
  }

  @Test
  void elPaginadorProvisorioDelAdminEsAhoraElComponente() throws Exception {
    mvc.perform(get("/admin-donantes"))
        .andExpect(content().string(containsString("class=\"paginas\"")))
        .andExpect(content().string(containsString("aria-label=\"Páginas de donantes\"")))
        .andExpect(content().string(containsString("aria-label=\"Página siguiente\"")))
        .andExpect(content().string(not(containsString("‹ Anterior"))));
  }
}
