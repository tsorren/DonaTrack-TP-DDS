package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadInicioPuerto;
import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.EntidadInicioVista;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HB-22: la donación que llegó a la puerta, como la da Donaciones (sin pedido que cubre ni
 * categoría), en "Esperan tu confirmación" de Inicio, Donaciones recibidas y Seguir entregas, con
 * el botón al acta de esa donación.
 */
@WebMvcTest(
    controllers = {
      EntidadInicioController.class,
      RecibidasController.class,
      EntidadSeguimientoController.class,
      PantallasController.class
    },
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadInicioEnPuertaControllerTest.UnaEnLaPuerta.class
})
class EntidadInicioEnPuertaControllerTest {

  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String DONACION = "6f1c2c1e-0000-4000-8000-000000000009";

  @TestConfiguration
  static class UnaEnLaPuerta {

    @Bean
    RecibidasPuerto recibidas() {
      return entidad ->
          new RecibidasVista(
              List.of(
                  new Recibida(
                      DONACION,
                      "Lentejas secas",
                      null,
                      null,
                      "15",
                      "un donante",
                      null,
                      null,
                      "puerta",
                      "Llegó a tu puerta el 8 de noviembre a las 18:20. Espera tu confirmación.",
                      null,
                      null,
                      "Llegó el 8 de noviembre, 18:20.",
                      "llegó a tu puerta el 8 de noviembre")),
              "noviembre",
              "8 de noviembre de 2026, 18:30",
              null);
    }

    @Bean
    EntidadInicioPuerto inicio() {
      return entidad ->
          new EntidadInicioVista(
              "domingo 8 de noviembre de 2026", "8 de noviembre, 18:30", List.of());
    }

    @Bean
    EntidadSeguimientoPuerto seguimientos() {
      return new EntidadSeguimientoPuerto() {
        @Override
        public Map<String, String> noticias(UUID entidad, Collection<String> claves) {
          return Map.of();
        }

        @Override
        public boolean dibujoDeEjemplo() {
          return false;
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @ParameterizedTest
  @ValueSource(strings = {"/entidad-inicio", "/entidad-recibidas", "/entidad-seguimiento"})
  void laQueLlegoEsperaLaConfirmacionConElBotonAlActaDeEsaDonacion(String pantalla)
      throws Exception {
    mvc.perform(get(pantalla).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lentejas secas")))
        .andExpect(
            content().string(containsString("entidad-seguimiento-confirmar?entrega=" + DONACION)))
        .andExpect(content().string(containsString("Confirmar")))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void enInicioDiceCuandoLlegoSinInventarElPedido() throws Exception {
    mvc.perform(get("/entidad-inicio").param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Llegó el 8 de noviembre, 18:20.")))
        .andExpect(content().string(containsString("1 entrega espera tu confirmación.")))
        .andExpect(content().string(not(containsString("Para tu necesidad de"))));
  }
}
