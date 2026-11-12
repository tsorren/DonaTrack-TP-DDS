package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteSeguimientoVista;
import grupo5.cliente.vistas.DonanteSeguimientoVista.Punto;
import grupo5.cliente.vistas.EstadoEntrega;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Seguimiento del donante contra respuestas de ejemplo de Donaciones y Logística. */
class DonanteSeguimientoRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String LENTEJAS = "6f1c2c1e-0000-4000-8000-000000000001";
  private static final String ARROZ = "6f1c2c1e-0000-4000-8000-000000000002";
  private static final String ENTREGA = "6f1c2c1e-0000-4000-8000-0000000000f1";

  /** Una en traslado hacia la entidad y una en el depósito, sin destino. */
  private static final String DOS =
      """
      [
        {"id": "%1$s", "descripcion": "Lentejas secas", "estadoActual": "EnTraslado",
         "fechaRegistro": "2026-10-12T10:00:00", "cantidad": 15, "entidadBeneficiariaId": "%3$s",
         "items": [{"cantidad": 15, "bien": {"categoria": {"id": "%3$s", "nombre": "Alimentos",
           "unidad": "KILOGRAMO"}}}],
         "historial": [
           {"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-10-13T11:00:00"},
           {"estadoNuevo": "EnTraslado", "timestamp": "2026-10-15T14:30:00"}]},
        {"id": "%2$s", "descripcion": "Arroz largo fino", "estadoActual": "EnDeposito",
         "fechaRegistro": "2026-10-14T17:00:00", "cantidad": 10}
      ]
      """
          .formatted(LENTEJAS, ARROZ, ENTIDAD);

  private MockRestServiceServer apis;
  private DonanteSeguimientoRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    // 19:25 en UTC son las 16:25 del 15 de octubre en la Argentina.
    Clock hoy = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    adaptador =
        new DonanteSeguimientoRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            hoy);
  }

  private void conDonaciones() {
    apis.expect(
            requestTo("http://donaciones/donaciones-independientes?size=100&donanteId=" + DONANTE))
        .andRespond(withSuccess(DOS, MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + ENTIDAD + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\"}]",
                MediaType.APPLICATION_JSON));
  }

  @Test
  void laQueVaEnCaminoTraeLaPosicionDelCamionYElDestino() {
    apis.expect(requestTo("http://logistica/api/entregas/seguimiento/donantes/" + DONANTE))
        .andRespond(
            withSuccess(
                "[{\"idEntrega\": \"%s\", \"idDonacion\": \"%s\", \"posicionCamion\":"
                        .formatted(ENTREGA, LENTEJAS)
                    + " {\"latitud\": -34.63, \"longitud\": -58.41,"
                    + " \"registradaEn\": \"2026-10-15T19:20:00Z\"}}]",
                MediaType.APPLICATION_JSON));
    conDonaciones();
    apis.expect(requestTo("http://logistica/api/entregas/" + ENTREGA))
        .andRespond(
            withSuccess(
                "{\"destino\": {\"latitud\": -34.64, \"longitud\": -58.42}}",
                MediaType.APPLICATION_JSON));

    DonanteSeguimientoVista vista = adaptador.seguimiento(DONANTE);

    apis.verify();
    assertEquals("hoy, 15 de octubre, a las 16:25", vista.actualizado());
    DonanteDonacionVista lentejas = vista.donaciones().get(1);
    assertEquals(EstadoEntrega.EN_CAMINO, lentejas.estado());
    assertEquals("Comedor Barrio Ejemplo", lentejas.destino());
    assertEquals("cat-alimentos", lentejas.categoria());
    assertEquals("Posición aproximada, actualizada hoy a las 16:20.", lentejas.seguimiento());
    assertEquals(new Punto(-34.63, -58.41, -34.64, -58.42), vista.puntos().get(LENTEJAS));
    assertEquals(
        "El camión salió a entregar tu donación a Comedor Barrio Ejemplo.",
        lentejas.cartas().getFirst().texto());
    assertNull(vista.donaciones().getFirst().destino());
  }

  @Test
  void sinLogisticaLaListaSeMuestraSinPosiciones() {
    apis.expect(requestTo("http://logistica/api/entregas/seguimiento/donantes/" + DONANTE))
        .andRespond(withServerError());
    conDonaciones();

    DonanteSeguimientoVista vista = adaptador.seguimiento(DONANTE);

    apis.verify();
    assertTrue(vista.puntos().isEmpty());
    assertNull(vista.donaciones().get(1).seguimiento());
    assertEquals(2, vista.donaciones().size());
  }
}
