package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Novedad;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Tema;
import grupo5.cliente.vistas.Tramo;
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
 * La bandeja con la página que da Notificaciones (R11-14): sin botón de confirmar, con todas las
 * categorías del rol; la entidad llega por {@code ?entidad=}.
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

  /** La entidad recién aprobada: sin novedades. */
  private static final UUID VACIA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000f0");

  @TestConfiguration
  static class DosNovedades {

    @Bean
    EntidadNotificacionesPuerto notificaciones() {
      return new EntidadNotificacionesPuerto() {
        @Override
        public EntidadNotificacionesVista bandeja(UUID entidad, ConsultaBandeja consulta) {
          List<Novedad> novedades =
              VACIA.equals(entidad)
                  ? List.of()
                  : List.of(
                      new Novedad(
                          "a",
                          "camino",
                          "2026-11-08T09:15",
                          false,
                          "Entrega en camino",
                          "Tu donación salió del depósito.",
                          "Ver en Donaciones recibidas",
                          null,
                          null,
                          "/entidad-recibidas",
                          null),
                      new Novedad(
                          "b",
                          "otras",
                          "2026-11-01T10:00",
                          true,
                          "Novedad",
                          "Llegó a tu puerta.",
                          "Ver en Donaciones recibidas",
                          null,
                          null,
                          "/entidad-recibidas",
                          null));
          return new EntidadNotificacionesVista(
              new Tramo<>(novedades, novedades.size(), 1, 1),
              novedades.isEmpty() ? 0 : 1,
              List.of(
                  new Tema("", "Todas las categorías", null),
                  new Tema("camino", "Entregas en camino", "camion"),
                  new Tema("problemas", "Problemas", "problema"),
                  new Tema("otras", "Otras novedades", "reloj")),
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
  void sinNovedadesLoDice() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", VACIA.toString()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no tenés novedades.")))
        .andExpect(content().string(not(containsString("Ninguna notificación coincide"))));
  }

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
        .andExpect(content().string(containsString("Entrega en camino")))
        .andExpect(content().string(containsString("Ver en Donaciones recibidas")))
        // Todas las categorías del rol, también las que no tienen avisos en la página.
        .andExpect(content().string(containsString("Problemas")))
        .andExpect(content().string(containsString("hace 9 horas")))
        .andExpect(content().string(not(containsString("Confirmar recepción"))));

    mvc.perform(get("/entidad-notificaciones-leidas").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Marcaste las novedades como leídas.")));
  }
}
