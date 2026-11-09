package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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
@Import({
  MaquetaConfig.class,
  RankingMaqueta.class,
  DonacionesMaqueta.class,
  RecibidasMaqueta.class,
  CuentaMaqueta.class,
  NotificacionesDonanteMaqueta.class,
  NotificacionesEntidadMaqueta.class
})
@ActiveProfiles("maqueta")
class VariantesInterceptorTest {

  private static final String PANTALLA = "/donante-perfil";

  @Autowired private MockMvc mvc;

  @Test
  void unaVarianteDeEstiloSumaSuHojaALaPantalla() throws Exception {
    mvc.perform(get(PANTALLA).param("variante", "prueba-a"))
        .andExpect(status().isOk())
        .andExpect(view().name("donante-perfil"))
        .andExpect(model().attribute("varianteCss", "prueba-a"))
        .andExpect(content().string(containsString("/css/variantes/prueba-a.css")));
  }

  @Test
  void unaVarianteDeEstructuraReemplazaLaPantalla() throws Exception {
    mvc.perform(get(PANTALLA).param("variante", "prueba-b"))
        .andExpect(status().isOk())
        .andExpect(view().name("variantes/prueba-b"))
        .andExpect(model().attributeDoesNotExist("varianteCss"));
  }

  @Test
  void unaVarianteDeEstructuraNoReemplazaUnaRedireccion() throws Exception {
    mvc.perform(post("/publico-contacto").param("variante", "prueba-b"))
        .andExpect(status().isFound())
        .andExpect(view().name("redirect:/publico-contacto"));
  }

  @Test
  void sinVarianteLaPantallaNoSumaHojas() throws Exception {
    mvc.perform(get(PANTALLA))
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist("varianteCss"));
  }

  @Test
  void unNombreQueNoEsUnArchivoDeVarianteNoExiste() throws Exception {
    mvc.perform(get(PANTALLA).param("variante", "../app")).andExpect(status().isNotFound());
  }

  @Test
  void unaVarianteSinArchivosNoExiste() throws Exception {
    mvc.perform(get(PANTALLA).param("variante", "tema-sin-archivos-a"))
        .andExpect(status().isNotFound());
  }
}
