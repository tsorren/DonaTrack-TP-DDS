package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Las variantes de prueba están en src/test/resources: prueba-a (estilo) y prueba-b (estructura).
 */
@WebMvcTest
@Import(MaquetaConfig.class)
@ActiveProfiles("maqueta")
class VariantesInterceptorTest {

  @Autowired private MockMvc mvc;

  @Test
  void elInicioLlevaAlCatalogo() throws Exception {
    mvc.perform(get("/")).andExpect(status().isFound()).andExpect(redirectedUrl("/estilos"));
  }

  @Test
  void unaVarianteDeEstiloSumaSuHojaALaPagina() throws Exception {
    mvc.perform(get("/estilos").param("variante", "prueba-a"))
        .andExpect(status().isOk())
        .andExpect(view().name("paginas/estilos"))
        .andExpect(model().attribute("varianteCss", "prueba-a"))
        .andExpect(content().string(containsString("/css/variantes/prueba-a.css")));
  }

  @Test
  void unaVarianteDeEstructuraReemplazaLaPagina() throws Exception {
    mvc.perform(get("/estilos").param("variante", "prueba-b"))
        .andExpect(status().isOk())
        .andExpect(view().name("variantes/prueba-b"))
        .andExpect(model().attributeDoesNotExist("varianteCss"));
  }

  @Test
  void unNombreQueNoEsUnArchivoDeVarianteNoExiste() throws Exception {
    mvc.perform(get("/estilos").param("variante", "../app")).andExpect(status().isNotFound());
  }

  @Test
  void unaVarianteSinArchivosNoExiste() throws Exception {
    mvc.perform(get("/estilos").param("variante", "paleta-a")).andExpect(status().isNotFound());
  }
}
