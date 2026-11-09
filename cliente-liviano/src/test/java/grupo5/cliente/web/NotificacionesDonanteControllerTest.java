package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.NotificacionesDonantePuerto;
import grupo5.cliente.vistas.NotificacionesDonanteVista;
import grupo5.cliente.vistas.NotificacionesDonanteVista.Noticia;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La pantalla de notificaciones del donante con los datos que manda Notificaciones (sin enlace al
 * detalle de donación ni sello); el donante llega por ?donante=.
 */
@WebMvcTest(
    controllers = {NotificacionesDonanteController.class, SitioController.class},
    properties = "cliente.datos.cuenta=parametro")
@Import({CuentaPorParametro.class, NotificacionesDonanteControllerTest.ConfigPuerto.class})
class NotificacionesDonanteControllerTest {

  private static final String DONANTE = "6f1c2c1e-0000-4000-8000-0000000000d1";

  static final AtomicBoolean MARCO_TODAS = new AtomicBoolean(false);

  @TestConfiguration
  static class ConfigPuerto {

    @Bean
    NotificacionesDonantePuerto notificacionesDonante() {
      return new NotificacionesDonantePuerto() {
        @Override
        public NotificacionesDonanteVista notificaciones(UUID donante) {
          return new NotificacionesDonanteVista(
              List.of(
                  new Noticia(
                      "El camión salió a entregar tus lentejas.",
                      "Hoy, 14:30",
                      "2026-11-08T14:30",
                      null,
                      null,
                      null)),
              List.of(
                  new Noticia(
                      "Recibimos tu donación en el depósito.",
                      "Ayer, 17:00",
                      "2026-11-07T17:00",
                      null,
                      null,
                      null)));
        }

        @Override
        public void marcarTodasLeidas(UUID donante) {
          MARCO_TODAS.set(true);
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinDonanteMandaAIngresar() throws Exception {
    mvc.perform(get("/donante-notificaciones")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraNotificacionesSinInventarSellosNiEnlacesQueNoVienen() throws Exception {
    mvc.perform(get("/donante-notificaciones").param("donante", DONANTE))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 sin leer")))
        .andExpect(content().string(containsString("El camión salió a entregar tus lentejas.")))
        .andExpect(content().string(containsString("Hoy, 14:30")))
        .andExpect(content().string(containsString("Recibimos tu donación en el depósito.")))
        .andExpect(content().string(containsString("Ayer, 17:00")))
        // Verifica que NO invente el enlace a donante-donaciones-detalle porque claveDonacion es
        // null
        .andExpect(content().string(not(containsString("href=\"/donante-donaciones-detalle"))))
        // Verifica que no pinte ningún sello inventado
        .andExpect(content().string(not(containsString("noticia-estado"))));
  }

  @Test
  void marcarTodasLeidasEjecutaElPuertoYRedirige() throws Exception {
    MARCO_TODAS.set(false);
    mvc.perform(post("/donante-notificaciones-leidas").param("donante", DONANTE))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/donante-notificaciones-leidas"));

    assertTrue(MARCO_TODAS.get(), "Debe haber llamado a marcarTodasLeidas en el puerto");
  }
}
