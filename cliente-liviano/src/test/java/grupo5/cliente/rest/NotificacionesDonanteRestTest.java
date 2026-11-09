package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.NotificacionesDonanteVista;
import grupo5.cliente.vistas.NotificacionesDonanteVista.Noticia;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST de notificaciones del donante contra respuestas de notificaciones-service. */
class NotificacionesDonanteRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000d1");

  /**
   * Tres notificaciones desordenadas: 1. Una sin leer de hoy a las 14:30. 2. Una leída de ayer a
   * las 17:00. 3. Una sin leer del 12 de octubre a las 10:00.
   */
  private static final String NOTIFICACIONES_JSON =
      """
            [
              {
                "id": "6f1c2c1e-0000-4000-8000-000000000001",
                "mensaje": "Recibimos tu donación en el depósito.",
                "estado": "PENDIENTE",
                "fechaCreacion": "2026-11-07T17:00:00",
                "leida": true,
                "fechaLectura": "2026-11-07T18:00:00"
              },
              {
                "id": "6f1c2c1e-0000-4000-8000-000000000002",
                "mensaje": "El camión salió a entregar tus lentejas.",
                "estado": "PENDIENTE",
                "fechaCreacion": "2026-11-08T14:30:00",
                "leida": false,
                "fechaLectura": null
              },
              {
                "id": "6f1c2c1e-0000-4000-8000-000000000003",
                "mensaje": "Se registró tu donación.",
                "estado": "PENDIENTE",
                "fechaCreacion": "2026-10-12T10:00:00",
                "leida": false,
                "fechaLectura": null
              }
            ]
            """;

  @Test
  void separaEnSinLeerYLeidasOrdenandoDeMasNuevaAVieja() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer notificaciones = MockRestServiceServer.bindTo(builder).build();

    notificaciones
        .expect(requestTo("http://notificaciones/api/notificaciones/persona/" + DONANTE))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(NOTIFICACIONES_JSON, MediaType.APPLICATION_JSON));

    // "Hoy" es el 8 de noviembre de 2026 a las 18:30 en Argentina (UTC-3 -> 21:30Z)
    Clock hoy = Clock.fixed(Instant.parse("2026-11-08T21:30:00Z"), ZoneOffset.UTC);
    NotificacionesDonanteRest adaptador =
        new NotificacionesDonanteRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class), hoy);

    NotificacionesDonanteVista vista = adaptador.notificaciones(DONANTE);

    notificaciones.verify();
    assertEquals(2, vista.sinLeer().size());
    assertEquals(1, vista.leidas().size());

    // La más nueva sin leer (de hoy)
    Noticia primeraSinLeer = vista.sinLeer().get(0);
    assertEquals("El camión salió a entregar tus lentejas.", primeraSinLeer.texto());
    assertEquals("Hoy, 14:30", primeraSinLeer.cuando());
    assertNull(primeraSinLeer.estado());
    assertNull(primeraSinLeer.claveDonacion());
    assertNull(primeraSinLeer.donacion());

    // La segunda sin leer (del 12 de octubre)
    Noticia segundaSinLeer = vista.sinLeer().get(1);
    assertEquals("Se registró tu donación.", segundaSinLeer.texto());
    assertEquals("12 de octubre, 10:00", segundaSinLeer.cuando());

    // La leída (de ayer)
    Noticia leida = vista.leidas().get(0);
    assertEquals("Recibimos tu donación en el depósito.", leida.texto());
    assertEquals("Ayer, 17:00", leida.cuando());
  }

  @Test
  void marcarTodasLeidasLlamaAlEndpointPatch() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer notificaciones = MockRestServiceServer.bindTo(builder).build();

    notificaciones
        .expect(
            requestTo(
                "http://notificaciones/api/notificaciones/persona/"
                    + DONANTE
                    + "/marcar-todas-leidas"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withNoContent());

    Clock hoy = Clock.systemUTC();
    NotificacionesDonanteRest adaptador =
        new NotificacionesDonanteRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class), hoy);

    adaptador.marcarTodasLeidas(DONANTE);

    notificaciones.verify();
  }
}
