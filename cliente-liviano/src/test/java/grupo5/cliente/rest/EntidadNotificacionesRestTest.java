package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.EntidadNotificacionesVista;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Novedad;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST de las notificaciones de la entidad contra respuestas de Notificaciones. */
class EntidadNotificacionesRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String UNA = "6f1c2c1e-0000-4000-8000-0000000000a1";
  private static final String OTRA = "6f1c2c1e-0000-4000-8000-0000000000a2";
  private static final String BASE = "http://notificaciones/api/notificaciones/";

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
  // "Ahora" es el 8 de noviembre de 2026, 18:30 en Buenos Aires.
  private final EntidadNotificacionesRest adaptador =
      new EntidadNotificacionesRest(
          ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class),
          Clock.fixed(Instant.parse("2026-11-08T21:30:45Z"), ZoneOffset.UTC));

  @Test
  void laBandejaLlevaFechaYLeidaSinInventarLaCategoriaNiLaDonacion() {
    servidor
        .expect(requestTo(BASE + "persona/" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "mensaje": "Tu donación salió del depósito.", "estado": "ENVIADA",
                  "fechaCreacion": "2026-11-08T09:15:30", "fechaLectura": null},
                 {"id": "%s", "mensaje": "Llegó a tu puerta.", "estado": "ENVIADA",
                  "fechaCreacion": "2026-11-01T10:00:00", "fechaLectura": "2026-11-02T08:00:00"},
                 {"id": null, "mensaje": "Sin id", "fechaCreacion": "2026-11-01T10:00:00"}]
                """
                    .formatted(UNA, OTRA),
                MediaType.APPLICATION_JSON));

    EntidadNotificacionesVista vista = adaptador.bandeja(ENTIDAD);

    servidor.verify();
    assertEquals("2026-11-08T18:30", vista.ahora());
    assertEquals("8 de noviembre de 2026, 18:30", vista.actualizado());
    assertEquals(List.of("", "novedades"), vista.temas().stream().map(t -> t.valor()).toList());
    assertEquals(2, vista.novedades().size());
    Novedad salio = vista.novedades().getFirst();
    assertEquals(UNA, salio.id());
    assertEquals("2026-11-08T09:15", salio.fecha());
    assertFalse(salio.leida());
    assertEquals("novedades", salio.tema());
    assertEquals("/entidad-recibidas", salio.destino());
    assertNull(salio.boton());
    assertTrue(vista.novedades().get(1).leida());
  }

  @Test
  void marcarLasElegidasSinNingunaTodasYDejarSinLeer() {
    servidor
        .expect(requestTo(BASE + UNA + "/leida?leida=true"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(BASE + "persona/" + ENTIDAD + "/marcar-todas-leidas"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess());
    servidor
        .expect(requestTo(BASE + OTRA + "/leida?leida=false"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    adaptador.marcarLeidas(ENTIDAD, List.of(UUID.fromString(UNA)));
    adaptador.marcarLeidas(ENTIDAD, List.of());
    adaptador.dejarSinLeer(UUID.fromString(OTRA));

    servidor.verify();
  }
}
