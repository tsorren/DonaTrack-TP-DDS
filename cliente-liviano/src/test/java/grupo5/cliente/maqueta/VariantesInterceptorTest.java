package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
 * La alternativa de prueba, en static/css/alternativas/prueba/.
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
    mvc.perform(get("/estilos").param("variante", "tema-sin-archivos-a"))
        .andExpect(status().isNotFound());
  }

  @Test
  void sinAlternativaLaPaginaUsaLasHojasDeGlassmorphism() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist("alternativa"))
        .andExpect(content().string(containsString("href=\"/css/tokens.css\"")))
        .andExpect(content().string(containsString("href=\"/css/app.css\"")));
  }

  @Test
  void unaAlternativaReemplazaLasHojasDeLaIdentidad() throws Exception {
    mvc.perform(get("/estilos").param("alternativa", "prueba").param("variante", "prueba-a"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("alternativa", "prueba"))
        .andExpect(content().string(containsString("/css/alternativas/prueba/tokens.css")))
        .andExpect(content().string(containsString("/css/alternativas/prueba/app.css")))
        .andExpect(content().string(not(containsString("href=\"/css/tokens.css\""))))
        .andExpect(content().string(containsString("/css/variantes/prueba-a.css")));
  }

  @Test
  void unNombreQueNoEsUnaCarpetaDeAlternativaNoExiste() throws Exception {
    mvc.perform(get("/estilos").param("alternativa", "../variantes"))
        .andExpect(status().isNotFound());
  }
}
