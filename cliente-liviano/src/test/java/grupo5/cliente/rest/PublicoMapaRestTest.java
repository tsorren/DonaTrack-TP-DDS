package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.PublicoMapaVista;
import grupo5.cliente.vistas.PublicoMapaVista.Estacion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** El mapa de entregadas contra respuestas de ejemplo del mapa público de Logística. */
class PublicoMapaRestTest {

  private static final String LOGISTICA = "http://logistica";

  private MockRestServiceServer apis;
  private PublicoMapaRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    Clock hoy = Clock.fixed(Instant.parse("2026-11-10T17:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new PublicoMapaRest(ClientesHttp.cliente(builder, LOGISTICA, LogisticaApi.class), hoy);
  }

  @Test
  void agrupaLasEntregasDelUltimoMesPorLugarYDiceLoQueNoSabe() {
    apis.expect(requestTo(LOGISTICA + "/api/entregas/mapa"))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "00000000-0000-4000-8000-000000000001", "latitud": -34.6037,
                  "longitud": -58.3816, "localidad": "Palermo", "provincia": "Buenos Aires",
                  "fechaEntrega": "2026-11-02T15:00:00Z", "pesoTotalKG": 12.5},
                 {"idEntrega": "00000000-0000-4000-8000-000000000002", "latitud": -34.60371,
                  "longitud": -58.38162, "localidad": "Palermo", "provincia": "Buenos Aires",
                  "fechaEntrega": "2026-11-09T15:00:00Z", "pesoTotalKG": 0},
                 {"idEntrega": "00000000-0000-4000-8000-000000000003", "latitud": -34.7,
                  "longitud": -58.5, "localidad": null, "provincia": null,
                  "fechaEntrega": "2026-10-20T15:00:00Z", "pesoTotalKG": 3},
                 {"idEntrega": "00000000-0000-4000-8000-000000000004", "latitud": -34.8,
                  "longitud": -58.6, "localidad": "Quilmes", "provincia": "Buenos Aires",
                  "fechaEntrega": "2026-09-01T15:00:00Z", "pesoTotalKG": 8},
                 {"idEntrega": "00000000-0000-4000-8000-000000000005", "latitud": -34.9,
                  "longitud": -58.7, "localidad": "Sin fecha", "fechaEntrega": null}]
                """,
                MediaType.APPLICATION_JSON));

    PublicoMapaVista mapa = adaptador.mapa();

    apis.verify();
    assertEquals(2, mapa.estaciones().size());
    Estacion palermo = mapa.estaciones().getFirst();
    assertEquals(1, palermo.numero());
    assertEquals("Palermo", palermo.nombre());
    assertEquals("Buenos Aires", palermo.barrio());
    assertEquals(-34.6037, palermo.lat(), 0.0001);
    assertTrue(palermo.pide().isEmpty());
    assertEquals(2, palermo.entregas().size());
    assertEquals("Entrega confirmada", palermo.entregas().getFirst().que());
    assertEquals("9 de noviembre", palermo.entregas().getFirst().dia());
    assertEquals("Entrega de 12,5 kg", palermo.entregas().get(1).que());
    assertEquals("2026-11-02", palermo.entregas().get(1).fecha());
    assertNull(palermo.entregas().getFirst().foto());
    assertNull(palermo.entregas().getFirst().cat());
    Estacion sinNombre = mapa.estaciones().get(1);
    assertEquals(2, sinNombre.numero());
    assertEquals("Lugar sin nombre", sinNombre.nombre());
    assertEquals("", sinNombre.barrio());
    assertEquals(3, mapa.todas().size());
    assertEquals("del 11 de octubre al 10 de noviembre de 2026", mapa.rango());
    assertEquals("10 de noviembre de 2026, 14:30", mapa.actualizado());
    assertFalse(mapa.conEntidades());
    assertFalse(mapa.conCategorias());
    assertTrue(mapa.nota().startsWith("Todavía no mostramos"));
  }

  @Test
  void sinEntregasElMapaVaVacio() {
    apis.expect(requestTo(LOGISTICA + "/api/entregas/mapa"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertTrue(adaptador.mapa().estaciones().isEmpty());
  }

  @Test
  void conLogisticaCaidaLaFallaSale() {
    apis.expect(requestTo(LOGISTICA + "/api/entregas/mapa")).andRespond(withServerError());

    assertThrows(RestClientException.class, adaptador::mapa);
  }
}
