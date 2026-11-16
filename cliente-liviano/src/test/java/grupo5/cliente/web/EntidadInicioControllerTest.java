package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadInicioPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.EntidadInicioVista;
import grupo5.cliente.vistas.EntidadInicioVista.Necesidad;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El Inicio con lo que dan las APIs: una en camino y una necesidad sin lo recibido; la entidad
 * llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadInicioController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadInicioControllerTest.DeLasApis.class
})
class EntidadInicioControllerTest {

  private static final String PANTALLA = "/entidad-inicio";

  @TestConfiguration
  static class DeLasApis {

    @Bean
    RecibidasPuerto recibidas() {
      return entidad ->
          new RecibidasVista(
              List.of(
                  new Recibida(
                      "a",
                      "Silla plegable",
                      null,
                      null,
                      "1",
                      "un donante",
                      null,
                      null,
                      "camino",
                      "Salió del depósito el 8 de noviembre a las 9:15.",
                      null,
                      null,
                      null,
                      "salió del depósito el 8 de noviembre")),
              "noviembre",
              "8 de noviembre de 2026, 18:30",
              null);
    }

    @Bean
    EntidadInicioPuerto inicio() {
      return entidad ->
          new EntidadInicioVista(
              "domingo 8 de noviembre de 2026",
              "8 de noviembre, 18:30",
              List.of(new Necesidad("Sillas", null, null, null, 6, "Hasta el 20 de noviembre")));
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLoDeLasApisSinInventarLoQueNoLlega() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("domingo 8 de noviembre de 2026")))
        .andExpect(content().string(containsString("No tenés entregas para")))
        .andExpect(content().string(containsString("salió del depósito el 8 de noviembre")))
        .andExpect(content().string(containsString("Sillas")))
        .andExpect(content().string(not(containsString("null"))))
        .andExpect(content().string(containsString("Hasta el 20 de noviembre")))
        .andExpect(content().string(containsString("Lo recibido en noviembre")))
        .andExpect(content().string(containsString("Al 8 de noviembre, 18:30.")))
        .andExpect(content().string(not(containsString("octubre"))));
  }
}
