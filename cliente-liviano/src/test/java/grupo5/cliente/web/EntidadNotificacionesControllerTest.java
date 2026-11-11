package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Novedad;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Tema;
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
 * La bandeja con lo que da Notificaciones: sin categoría propia ni botón de confirmar; la entidad
 * llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadNotificacionesController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadNotificacionesControllerTest.DosNovedades.class
})
class EntidadNotificacionesControllerTest {

  private static final String PANTALLA = "/entidad-notificaciones";

  @TestConfiguration
  static class DosNovedades {

    @Bean
    EntidadNotificacionesPuerto notificaciones() {
      return new EntidadNotificacionesPuerto() {
        @Override
        public EntidadNotificacionesVista bandeja(UUID entidad) {
          return new EntidadNotificacionesVista(
              List.of(
                  new Novedad(
                      "a",
                      "novedades",
                      "2026-11-08T09:15",
                      false,
                      "Tu donación salió del depósito.",
                      null,
                      null,
                      "/entidad-recibidas",
                      null),
                  new Novedad(
                      "b",
                      "novedades",
                      "2026-11-01T10:00",
                      true,
                      "Llegó a tu puerta.",
                      null,
                      null,
                      "/entidad-recibidas",
                      null)),
              List.of(
                  new Tema("", "Todas las categorías", null),
                  new Tema("novedades", "Novedades", "reloj")),
              "2026-11-08T18:30",
              "8 de noviembre de 2026, 18:30");
        }

        @Override
        public void marcarLeidas(UUID entidad, List<UUID> ids) {
          // No hace falta en este test.
        }

        @Override
        public void dejarSinLeer(UUID id) {
          // No hace falta en este test.
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLoDeNotificacionesSinInventarLoQueNoLlega() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(
            get(PANTALLA).param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1").session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("1 sin leer. Al 8 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Tu donación salió del depósito.")))
        .andExpect(content().string(containsString("Novedades")))
        .andExpect(content().string(containsString("hace 9 horas")))
        .andExpect(content().string(not(containsString("Confirmar recepción"))));

    mvc.perform(get("/entidad-notificaciones-leidas").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Marcaste las novedades como leídas.")));
  }
}
