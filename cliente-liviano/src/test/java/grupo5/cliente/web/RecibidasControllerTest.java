package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La pantalla con lo que da Donaciones: sin pedido que cubre, una sin categoría, sin el aviso de
 * novedades y con el mes de las recibidas; la entidad llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {RecibidasController.class, SitioController.class},
    properties = "cliente.datos.cuenta=parametro")
@Import({CuentaPorParametro.class, RecibidasControllerTest.DosAsignadas.class})
class RecibidasControllerTest {

  private static final String PANTALLA = "/entidad-recibidas";
  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";

  @TestConfiguration
  static class DosAsignadas {

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
                      null),
                  new Recibida(
                      "b",
                      "Fideos secos",
                      "cat-alimentos",
                      "Alimentos",
                      "8 kg",
                      "Martina Ruiz",
                      null,
                      null,
                      "recibida",
                      "Confirmaste la recepción el 5 de noviembre.",
                      "5",
                      "2026-11-05")),
              "noviembre",
              "8 de noviembre de 2026, 18:30",
              null);
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLoDeDonacionesSinInventarLoQueNoLlega() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Al 8 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Recibidas en noviembre (1)")))
        .andExpect(content().string(containsString("<b>5</b> de noviembre")))
        .andExpect(content().string(containsString("Alimentos · Donó Martina Ruiz")))
        .andExpect(content().string(containsString("Donó un donante")))
        .andExpect(content().string(containsString("<p class=\"acta-cuanto\">1</p>")))
        .andExpect(content().string(not(containsString("Para tu pedido"))))
        .andExpect(content().string(not(containsString("novedades sin leer"))))
        .andExpect(content().string(not(containsString("Confirmar recepción"))));

    // La sesión recuerda la entidad, y el filtro deja solo ese grupo.
    mvc.perform(get(PANTALLA).param("estado", "en-camino").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Mostrando: En camino (1).")))
        .andExpect(content().string(not(containsString("Fideos secos"))));
  }
}
