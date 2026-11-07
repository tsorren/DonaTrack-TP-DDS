package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Categoria;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
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
 * La pantalla con lo que da Donaciones: categorías por id, una sin ícono, y la cuenta por {@code
 * ?donante=}, que la sesión recuerda.
 */
@WebMvcTest(
    controllers = {MisDonacionesController.class, SitioController.class},
    properties = "cliente.datos.cuenta=parametro")
@Import({CuentaPorParametro.class, MisDonacionesControllerTest.DosDonaciones.class})
class MisDonacionesControllerTest {

  private static final String ALIMENTOS = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String MUEBLES = "6f1c2c1e-0000-4000-8000-0000000000c2";
  private static final String DONANTE = "6f1c2c1e-0000-4000-8000-0000000000aa";

  @TestConfiguration
  static class DosDonaciones {

    @Bean
    DonacionesPuerto donaciones() {
      return donante ->
          new MisDonacionesVista(
              List.of(
                  new Donacion(
                      "b",
                      2,
                      "Silla plegable",
                      "1 unidades",
                      MUEBLES,
                      null,
                      "Mobiliario",
                      null,
                      "14 de octubre de 2026",
                      EstadoEntrega.PENDIENTE,
                      "En el depósito, espera destino"),
                  new Donacion(
                      "a",
                      1,
                      "Fideos secos",
                      "8 kg",
                      ALIMENTOS,
                      "cat-alimentos",
                      "Alimentos",
                      "Comedor Barrio Ejemplo",
                      "1 de octubre de 2026",
                      EstadoEntrega.ENTREGADA,
                      "Entrega confirmada el 5 de octubre")),
              List.of(new Categoria(ALIMENTOS, "Alimentos"), new Categoria(MUEBLES, "Mobiliario")),
              "hoy, 7 de noviembre, 19:40");
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinCuentaOConUnIdMalEscritoMandaAIngresar() throws Exception {
    mvc.perform(get("/donante-donaciones")).andExpect(redirectedUrl("/publico-ingresar"));
    mvc.perform(get("/donante-donaciones?donante=no-es-un-id"))
        .andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void laSesionRecuerdaAlDonanteYElFiltroUsaLasCategoriasDeLaApi() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "2 donaciones, de la más reciente a la primera. Actualizado hoy, 7 de"
                            + " noviembre, 19:40.")))
        .andExpect(content().string(containsString("Mobiliario")));

    mvc.perform(get("/donante-donaciones?categoria=" + ALIMENTOS).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(content().string(containsString("Fideos secos")))
        .andExpect(content().string(not(containsString("Silla plegable"))));
  }
}
