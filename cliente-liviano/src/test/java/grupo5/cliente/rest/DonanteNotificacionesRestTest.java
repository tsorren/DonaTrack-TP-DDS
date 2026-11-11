package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.DonanteNotificacionesVista;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Aviso;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** La bandeja del donante contra respuestas de ejemplo de Notificaciones. */
class DonanteNotificacionesRestTest {

  private static final UUID PERSONA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String UNA = "6f1c2c1e-0000-4000-8000-0000000000b1";
  private static final String OTRA = "6f1c2c1e-0000-4000-8000-0000000000b2";
  private static final String BANDEJA =
      "http://notificaciones/api/notificaciones/persona/" + PERSONA;

  /** Desordenadas, una leída; textos como los arma Notificaciones. */
  private static final String DOS =
      """
      [
        {"id": "%1$s", "mensaje": "Tu donación de Fideos fue asignada a Comedor Barrio Ejemplo",
         "estado": "ENVIADA", "fechaCreacion": "2026-10-14T12:15:30", "fechaLectura": null},
        {"id": "%2$s", "mensaje": "¡Felicitaciones! Has ascendido de la categoría A a B.",
         "estado": "ENVIADA", "fechaCreacion": "2026-10-15T16:02:00",
         "fechaLectura": "2026-10-15T16:10:00"}
      ]
      """
          .formatted(UNA, OTRA);

  private MockRestServiceServer apis;
  private DonanteNotificacionesRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    // 19:25 en UTC son las 16:25 en la Argentina.
    Clock hoy = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    adaptador =
        new DonanteNotificacionesRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class), hoy);
  }

  @Test
  void armaLaBandejaDeLaMasNuevaALaMasViejaConSuCategoria() {
    apis.expect(requestTo(BANDEJA)).andRespond(withSuccess(DOS, MediaType.APPLICATION_JSON));

    DonanteNotificacionesVista vista = adaptador.bandeja(PERSONA);

    apis.verify();
    assertEquals("2026-10-15T16:25", vista.ahora());
    Aviso ascenso = vista.avisos().get(0);
    assertEquals(OTRA, ascenso.id());
    assertEquals("otras", ascenso.tema());
    assertTrue(ascenso.leida());
    Aviso asignada = vista.avisos().get(1);
    assertEquals("2026-10-14T12:15", asignada.fecha());
    assertEquals("asignadas", asignada.tema());
    assertEquals("/donante-donaciones", asignada.destino());
    assertFalse(asignada.leida());
  }

  @Test
  void laCategoriaSaleDelTexto() {
    assertEquals("problemas", DonanteNotificacionesRest.tema("No pudimos entregar tu donación"));
    assertEquals("entregadas", DonanteNotificacionesRest.tema("¡Gracias! X recibió tu donación"));
    assertEquals(
        "camino", DonanteNotificacionesRest.tema("El camión salió a entregar tu donación"));
    assertEquals("necesidades", DonanteNotificacionesRest.tema("X publicó una necesidad nueva"));
    assertEquals(
        "deposito", DonanteNotificacionesRest.tema("Recibimos tu donación en el depósito."));
  }

  @Test
  void marcarSoloTocaLasDeSuBandeja() {
    apis.expect(once(), requestTo(BANDEJA))
        .andRespond(withSuccess(DOS, MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://notificaciones/api/notificaciones/" + UNA + "/leida?leida=true"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    adaptador.marcar(PERSONA, List.of(UNA, "6f1c2c1e-0000-4000-8000-0000000000ff", "x"), true);

    apis.verify();
  }

  @Test
  void marcarTodasUsaLaDeLaPersona() {
    apis.expect(requestTo(BANDEJA + "/marcar-todas-leidas"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withSuccess());

    adaptador.marcarTodas(PERSONA);

    apis.verify();
  }
}
