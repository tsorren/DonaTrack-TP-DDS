package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.NotificacionesEntidadVista;
import grupo5.cliente.vistas.NotificacionesEntidadVista.Novedad;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * El adaptador REST de notificaciones de la entidad contra respuestas de notificaciones-service.
 */
class NotificacionesEntidadRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");

  /**
   * Tres notificaciones desordenadas: 1. Una leída de ayer a las 17:10. 2. Una sin leer de hoy a
   * las 18:20. 3. Una sin leer del 13 de octubre a las 10:05.
   */
  private static final String NOTIFICACIONES_JSON =
      """
      [
        {
          "id": "6f1c2c1e-0000-4000-8000-000000000001",
          "mensaje": "Ropa de abrigo, 10 unidades, llegó a tu puerta.",
          "estado": "PENDIENTE",
          "fechaCreacion": "2026-11-07T17:10:00",
          "leida": true,
          "fechaLectura": "2026-11-07T18:00:00"
        },
        {
          "id": "6f1c2c1e-0000-4000-8000-000000000002",
          "mensaje": "Lentejas secas, 15 kg, llegó a tu puerta.",
          "estado": "PENDIENTE",
          "fechaCreacion": "2026-11-08T18:20:00",
          "leida": false,
          "fechaLectura": null
        },
        {
          "id": "6f1c2c1e-0000-4000-8000-000000000003",
          "mensaje": "Se asignó a tu pedido de leche en polvo una donación de 12 unidades.",
          "estado": "PENDIENTE",
          "fechaCreacion": "2026-10-13T10:05:00",
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
        .expect(requestTo("http://notificaciones/api/notificaciones/persona/" + ENTIDAD))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(NOTIFICACIONES_JSON, MediaType.APPLICATION_JSON));

    // "Hoy" es el 8 de noviembre de 2026 a las 18:30 en Argentina (UTC-3 -> 21:30Z)
    Clock hoy = Clock.fixed(Instant.parse("2026-11-08T21:30:00Z"), ZoneOffset.UTC);
    NotificacionesEntidadRest adaptador =
        new NotificacionesEntidadRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class), hoy);

    NotificacionesEntidadVista vista = adaptador.novedades(ENTIDAD);

    notificaciones.verify();
    assertEquals(2, vista.sinLeer().size());
    assertEquals(1, vista.leidas().size());
    assertEquals("8 de noviembre de 2026, 18:30", vista.actualizado());
    assertEquals(2, vista.marcadasRecien());

    // La más nueva sin leer (de hoy)
    Novedad primeraSinLeer = vista.sinLeer().get(0);
    assertEquals("Lentejas secas, 15 kg, llegó a tu puerta.", primeraSinLeer.texto());
    assertEquals("Hoy", primeraSinLeer.dia());
    assertEquals("18:20", primeraSinLeer.hora());
    assertNull(primeraSinLeer.tipo());
    assertNull(primeraSinLeer.tono());
    assertNull(primeraSinLeer.confirmo());
    assertNull(primeraSinLeer.enlace());
    assertNull(primeraSinLeer.destino());
    assertNull(primeraSinLeer.que());
    assertFalse(primeraSinLeer.principal());
    assertFalse(primeraSinLeer.leida());

    // La segunda sin leer (del 13 de octubre)
    Novedad segundaSinLeer = vista.sinLeer().get(1);
    assertEquals(
        "Se asignó a tu pedido de leche en polvo una donación de 12 unidades.",
        segundaSinLeer.texto());
    assertEquals("13 de octubre", segundaSinLeer.dia());
    assertEquals("10:05", segundaSinLeer.hora());

    // La leída (de ayer)
    Novedad leida = vista.leidas().get(0);
    assertEquals("Ropa de abrigo, 10 unidades, llegó a tu puerta.", leida.texto());
    assertEquals("Ayer", leida.dia());
    assertEquals("17:10", leida.hora());
  }

  @Test
  void marcarTodasLeidasLlamaAlEndpointPatch() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer notificaciones = MockRestServiceServer.bindTo(builder).build();

    notificaciones
        .expect(
            requestTo(
                "http://notificaciones/api/notificaciones/persona/"
                    + ENTIDAD
                    + "/marcar-todas-leidas"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withNoContent());

    Clock hoy = Clock.systemUTC();
    NotificacionesEntidadRest adaptador =
        new NotificacionesEntidadRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class), hoy);

    adaptador.marcarTodasLeidas(ENTIDAD);

    notificaciones.verify();
  }
}
