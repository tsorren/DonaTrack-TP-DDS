package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.DonanteInicioVista;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El inicio del donante contra respuestas de ejemplo de Donaciones, Logística y Notificaciones. */
class DonanteInicioRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";

  /** Una entregada en octubre, una en septiembre y una en el depósito. */
  private static final String TRES =
      """
      [
        {"id": "6f1c2c1e-0000-4000-8000-000000000001", "descripcion": "Fideos secos",
         "estadoActual": "Entregada", "fechaRegistro": "2026-10-01T10:00:00", "cantidad": 8,
         "entidadBeneficiariaId": "%1$s",
         "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-10-05T17:20:00"}]},
        {"id": "6f1c2c1e-0000-4000-8000-000000000002", "descripcion": "Ropa de abrigo",
         "estadoActual": "Entregada", "fechaRegistro": "2026-09-01T10:00:00", "cantidad": 12,
         "entidadBeneficiariaId": "%1$s",
         "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-09-09T18:05:00"}]},
        {"id": "6f1c2c1e-0000-4000-8000-000000000003", "descripcion": "Arroz largo fino",
         "estadoActual": "EnDeposito", "fechaRegistro": "2026-10-14T17:00:00", "cantidad": 10,
         "historial": [{"estadoNuevo": "EnDeposito", "timestamp": "2026-10-14T17:00:00"}]}
      ]
      """
          .formatted(ENTIDAD);

  private MockRestServiceServer apis;
  private DonanteInicioRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    // 19:25 en UTC son las 16:25 del 15 de octubre en la Argentina.
    Clock hoy = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    adaptador =
        new DonanteInicioRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class),
            hoy);
  }

  @Test
  void armaLasCifrasDelMesLasNoticiasYLasSinLeer() {
    apis.expect(requestTo("http://logistica/api/entregas/seguimiento/donantes/" + DONANTE))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/donaciones-independientes?donanteId=" + DONANTE))
        .andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/entidades/publicas"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + ENTIDAD + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\"}]",
                MediaType.APPLICATION_JSON));
    apis.expect(
            requestTo(
                "http://notificaciones/api/notificaciones/persona/"
                    + DONANTE
                    + "/no-leidas/conteo"))
        .andRespond(withSuccess("{\"noLeidas\": 2}", MediaType.APPLICATION_JSON));

    DonanteInicioVista inicio = adaptador.inicio(DONANTE);

    apis.verify();
    assertEquals(3, inicio.donaciones().size());
    assertEquals(1, inicio.entregadasDelMes());
    assertEquals(2, inicio.sinLeer());
    assertEquals("octubre de 2026", inicio.mes());
    assertEquals("Octubre de 2026", inicio.mesConMayuscula());
    assertEquals("octubre", inicio.nombreDelMes());
    assertEquals("15 de octubre", inicio.hoy());
    assertEquals("15 de octubre, 16:25", inicio.actualizado());
    assertEquals("1 de septiembre", inicio.desde());
    assertEquals("2026-10-14", inicio.ayerIso());
    assertEquals(3, inicio.avisos().size());
    assertEquals("Arroz largo fino", inicio.avisos().getFirst().d().titulo());
  }

  @Test
  void sinNotificacionesNiLogisticaElInicioSaleIgual() {
    apis.expect(requestTo("http://logistica/api/entregas/seguimiento/donantes/" + DONANTE))
        .andRespond(withServerError());
    apis.expect(requestTo("http://donaciones/donaciones-independientes?donanteId=" + DONANTE))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    apis.expect(
            requestTo(
                "http://notificaciones/api/notificaciones/persona/"
                    + DONANTE
                    + "/no-leidas/conteo"))
        .andRespond(withServerError());

    DonanteInicioVista inicio = adaptador.inicio(DONANTE);

    assertEquals(0, inicio.sinLeer());
    assertEquals("15 de octubre", inicio.desde());
  }
}
