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
import grupo5.cliente.puertos.NotificacionesEntidadPuerto;
import grupo5.cliente.vistas.NotificacionesEntidadVista;
import grupo5.cliente.vistas.NotificacionesEntidadVista.Novedad;
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
 * La pantalla de notificaciones de la entidad con los datos que manda Notificaciones (sin sello ni
 * enlaces a entregas o necesidades); la entidad llega por ?entidad=.
 */
@WebMvcTest(
    controllers = {NotificacionesEntidadController.class, SitioController.class},
    properties = "cliente.datos.cuenta=parametro")
@Import({CuentaPorParametro.class, NotificacionesEntidadControllerTest.ConfigPuerto.class})
class NotificacionesEntidadControllerTest {

  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String BANDEJA = "/entidad-notificaciones";
  private static final String LEIDAS = "/entidad-notificaciones-leidas";
  private static final String INGRESAR = "/publico-ingresar";

  static final AtomicBoolean MARCO_TODAS = new AtomicBoolean(false);

  @TestConfiguration
  static class ConfigPuerto {

    @Bean
    NotificacionesEntidadPuerto notificacionesEntidad() {
      return new NotificacionesEntidadPuerto() {
        @Override
        public NotificacionesEntidadVista novedades(UUID entidad) {
          return new NotificacionesEntidadVista(
              List.of(
                  new Novedad(
                      "n1",
                      null,
                      null,
                      "Hoy",
                      "18:20",
                      "2026-11-08T18:20",
                      "Lentejas secas, 15 kg, llegó a tu puerta.",
                      null,
                      null,
                      null,
                      null,
                      false,
                      false)),
              List.of(
                  new Novedad(
                      "n2",
                      null,
                      null,
                      "Ayer",
                      "17:10",
                      "2026-11-07T17:10",
                      "Ropa de abrigo, 10 unidades, llegó a tu puerta.",
                      null,
                      null,
                      null,
                      null,
                      false,
                      true)),
              "8 de noviembre de 2026, 18:30",
              1);
        }

        @Override
        public void marcarTodasLeidas(UUID entidad) {
          MARCO_TODAS.set(true);
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(BANDEJA)).andExpect(redirectedUrl(INGRESAR));
    mvc.perform(get(LEIDAS)).andExpect(redirectedUrl(INGRESAR));
    mvc.perform(post(LEIDAS)).andExpect(redirectedUrl(INGRESAR));
  }

  @Test
  void muestraNovedadesSinInventarSellosNiEnlacesQueNoVienen() throws Exception {
    mvc.perform(get(BANDEJA).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("1 sin leer. Al 8 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Lentejas secas, 15 kg, llegó a tu puerta.")))
        .andExpect(content().string(containsString("<b>Hoy</b>")))
        .andExpect(content().string(containsString("18:20")))
        .andExpect(
            content().string(containsString("Ropa de abrigo, 10 unidades, llegó a tu puerta.")))
        .andExpect(content().string(containsString("<b>Ayer</b>")))
        .andExpect(content().string(containsString("17:10")))
        .andExpect(content().string(not(containsString("novedad-tipo"))))
        .andExpect(content().string(not(containsString("Confirmar recepción"))))
        .andExpect(content().string(not(containsString("novedad-enlace"))));

    mvc.perform(get(BANDEJA).param("entidad", ENTIDAD).param("escenario", "nada-sin-leer"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No tenés novedades sin leer.")));

    mvc.perform(get(LEIDAS).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Marcaste las 3 como leídas.")));
  }

  @Test
  void marcarTodasLeidasEjecutaElPuertoYRedirige() throws Exception {
    MARCO_TODAS.set(false);
    mvc.perform(post(LEIDAS).param("entidad", ENTIDAD))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(LEIDAS));

    assertTrue(MARCO_TODAS.get(), "Debe haber llamado a marcarTodasLeidas en el puerto");
  }
}
