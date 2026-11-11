package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.vistas.EntidadEntregaVista;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confirmar recepción con lo que dan Logística y Donaciones: una entrega sin remito, sin lo
 * recibido de la necesidad y de quien ocultó su nombre; la entidad llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadConfirmarController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadConfirmarControllerTest.UnaEntrega.class
})
class EntidadConfirmarControllerTest {

  private static final String ENTREGA = "6f1c2c1e-0000-4000-8000-0000000000a1";

  @TestConfiguration
  static class UnaEntrega {

    @Bean
    EntidadConfirmarPuerto confirmar() {
      return new EntidadConfirmarPuerto() {
        @Override
        public List<EntidadEntregaVista> entregas(UUID entidad) {
          return List.of(
              new EntidadEntregaVista(
                  ENTREGA,
                  "silla plegable",
                  "Silla plegable, 1 unidad",
                  "1 unidad",
                  "8 de noviembre, 14:30",
                  null,
                  "Salió del depósito el 8 de noviembre a las 14:30.",
                  "Salió del depósito el 8 de noviembre a las 14:30.",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  1,
                  "unidades",
                  false,
                  null,
                  null));
        }

        @Override
        public String hoy() {
          return "8 de noviembre de 2026";
        }

        @Override
        public void confirmar(UUID entidad, String clave, Acta acta) {
          // No hace falta en este test.
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get("/entidad-seguimiento-confirmar"))
        .andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void elActaDeUnaEntregaDeLasApis() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(
            get("/entidad-seguimiento-confirmar")
                .param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1")
                .param("entrega", ENTREGA)
                .session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Silla plegable, 1 unidad. Salió del depósito el 8 de")))
        .andExpect(
            content().string(containsString("action=\"/entidad-seguimiento-sellada-" + ENTREGA)))
        .andExpect(
            content()
                .string(containsString("hx-indicator=\"#enviando-entidad-seguimiento-confirmar\"")))
        .andExpect(content().string(containsString(", el 8 de noviembre de 2026</p>")))
        .andExpect(content().string(containsString("Un donante")))
        .andExpect(content().string(not(containsString("null"))))
        .andExpect(content().string(not(containsString("15 de octubre"))));

    mvc.perform(get("/entidad-seguimiento-problema").param("entrega", ENTREGA).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Cuánto llegó, en unidades")));

    mvc.perform(get("/entidad-seguimiento-confirmar").param("entrega", "otra").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa entrega")));
  }
}
