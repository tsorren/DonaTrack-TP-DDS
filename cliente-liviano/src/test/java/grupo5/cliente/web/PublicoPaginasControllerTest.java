package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.vistas.PublicoCifra;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Las páginas públicas con lo que da el puerto, no con los datos de ejemplo de la plantilla. */
@WebMvcTest({PublicoPortadaController.class, PublicoNosotrosController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, PublicoPaginasControllerTest.DatosDeLaApi.class})
class PublicoPaginasControllerTest {

  @TestConfiguration
  static class DatosDeLaApi {

    @Bean
    PublicoPuerto publico() {
      List<PublicoCifra> cifras =
          List.of(
              new PublicoCifra("7", "Entregas confirmadas"),
              new PublicoCifra("3", "Entidades en el mapa"),
              new PublicoCifra("5", "Donantes con al menos una donación"));
      return new PublicoPuerto() {
        @Override
        public List<PublicoCifra> cifrasDePortada() {
          return cifras;
        }

        @Override
        public List<PublicoCifra> cifrasDeNosotros() {
          return cifras;
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void laPortadaEnSusDosDireccionesMuestraLasCifrasDelPuerto() throws Exception {
    for (String ruta : List.of("/", "/publico-portada")) {
      mvc.perform(get(ruta))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("<b>7</b>")))
          .andExpect(content().string(containsString("Donantes con al menos una donación")))
          .andExpect(content().string(not(containsString("1.284"))));
    }
  }

  @Test
  void nosotrosMuestraLasCifrasDelPuerto() throws Exception {
    mvc.perform(get("/publico-nosotros"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<b>3</b>")))
        .andExpect(content().string(not(containsString("318"))));
  }
}
