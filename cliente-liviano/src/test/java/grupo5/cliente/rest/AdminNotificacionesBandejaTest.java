package grupo5.cliente.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.AdminNotificacionesPuerto;
import grupo5.cliente.web.AdminNotificacionesController;
import grupo5.cliente.web.Pantallas;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

/**
 * C17 (spec R11-p2-campanita): la pantalla admin-notificaciones con el adaptador real contra un
 * Notificaciones de mentira. Muestra los avisos de la Administración de a 20, con el paginador y
 * las categorías del admin, y ningún aviso de ejemplo.
 */
@WebMvcTest(controllers = AdminNotificacionesController.class)
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  AdminNotificacionesBandejaTest.ConNotificaciones.class
})
class AdminNotificacionesBandejaTest {

  private static final UUID ADMINISTRACION = UUID.fromString(AvisosRest.ADMINISTRACION);
  private static final String BASE =
      "http://notificaciones/api/notificaciones/persona/" + ADMINISTRACION;

  private static final RestClient.Builder BUILDER = RestClient.builder();
  private static final MockRestServiceServer SERVIDOR =
      MockRestServiceServer.bindTo(BUILDER).build();

  @TestConfiguration
  static class ConNotificaciones {

    @Bean
    AdminNotificacionesPuerto notificaciones() {
      return new AdminNotificacionesRest(
          ClientesHttp.cliente(BUILDER, "http://notificaciones", NotificacionesApi.class),
          ADMINISTRACION,
          Clock.fixed(Instant.parse("2026-11-12T21:45:00Z"), ZoneOffset.UTC));
    }
  }

  @Autowired private MockMvc mvc;

  @AfterEach
  void verificar() {
    SERVIDOR.verify();
    SERVIDOR.reset();
  }

  private static String veinte() {
    return IntStream.rangeClosed(1, 20)
        .mapToObj(
            i ->
                """
                {"id": "6f1c2c1e-0000-4000-8000-%012d", "mensaje": "La importación de donantes %d terminó.",
                 "fechaCreacion": "2026-11-12T10:00:00", "fechaLectura": null,
                 "tipoEvento": "IMPORTACION_TERMINADA",
                 "referenciaId": "7d1a2b3c-0000-4000-8000-%012d"}
                """
                    .formatted(i, i, i))
        .collect(Collectors.joining(",", "[", "]"));
  }

  @Test
  void laBandejaDelAdminEsLaDeLaAdministracionDeA20SinEjemplos() throws Exception {
    HttpHeaders total = new HttpHeaders();
    total.add("X-Total-Count", "45");
    SERVIDOR
        .expect(requestTo(BASE + "?page=0&size=20"))
        .andRespond(withSuccess(veinte(), MediaType.APPLICATION_JSON).headers(total));
    SERVIDOR
        .expect(requestTo(BASE + "/no-leidas/conteo"))
        .andRespond(withSuccess("{\"noLeidas\": 45}", MediaType.APPLICATION_JSON));

    mvc.perform(get("/admin-notificaciones"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("45 sin leer. Al 12 de noviembre de 2026, 18:45.")))
        .andExpect(content().string(containsString("Son los mismos para todas las cuentas")))
        .andExpect(content().string(containsString("La importación de donantes 20 terminó.")))
        .andExpect(content().string(containsString("Terminó una importación")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-donantes-importado?archivo=7d1a2b3c-0000-4000-8000-000000000001\"")))
        .andExpect(content().string(containsString("Mostrando 1 a 20 de 45 notificaciones.")))
        .andExpect(content().string(containsString("Páginas de notificaciones")))
        // Las tres categorías del admin y ninguna de las de ejemplo.
        .andExpect(content().string(containsString("Importaciones")))
        .andExpect(content().string(containsString("Mensajes de contacto")))
        .andExpect(content().string(containsString("Otras novedades")))
        .andExpect(content().string(not(containsString("Organizaciones para aprobar"))))
        .andExpect(content().string(not(containsString("Donaciones por vencer"))))
        .andExpect(content().string(not(containsString("donantes-octubre.csv"))))
        .andExpect(content().string(not(containsString("Hogar Ejemplo Norte"))));
  }

  @Test
  void sinAvisosDiceQueNoHayNotificacionesParaLaAdministracion() throws Exception {
    HttpHeaders total = new HttpHeaders();
    total.add("X-Total-Count", "0");
    SERVIDOR
        .expect(requestTo(BASE + "?page=0&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total));
    SERVIDOR
        .expect(requestTo(BASE + "/no-leidas/conteo"))
        .andRespond(withSuccess("{\"noLeidas\": 0}", MediaType.APPLICATION_JSON));

    mvc.perform(get("/admin-notificaciones"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Todavía no hay notificaciones para la administración.")))
        .andExpect(content().string(not(containsString("Mostrando"))));
  }
}
