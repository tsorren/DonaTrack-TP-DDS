package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST de Donaciones recibidas contra respuestas de ejemplo de Donaciones. */
class RecibidasRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String ALIMENTOS = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String EMPRESA = "6f1c2c1e-0000-4000-8000-0000000000d1";
  private static final String PERSONA = "6f1c2c1e-0000-4000-8000-0000000000d2";
  private static final String BORRADA = "6f1c2c1e-0000-4000-8000-0000000000d3";
  private static final String SALIR = "salir";
  private static final String ORIGINAL = "http://donaciones/api/donaciones/";
  private static final String SIN_DONANTE = "6f1c2c1e-0000-4000-8000-0000000000d4";
  private static final String SIN_NOMBRE = "6f1c2c1e-0000-4000-8000-0000000000d5";

  /**
   * Ocho asignadas, desordenadas: una entregada este mes y otra el anterior (no se muestra), una en
   * traslado, una lista para entregar, una vencida, una que no pudo llegar, una asignada sin
   * categoría y una en depósito. Dos vienen de la misma donación original, otra de una que ya no
   * está, otra de una sin donante y otra de una persona sin nombre.
   */
  private static final String OCHO =
      """
      [
        {"id": "6f1c2c1e-0000-4000-8000-000000000001", "descripcion": "Fideos secos",
         "estadoActual": "Entregada", "fechaRegistro": "2026-10-01T10:00:00",
         "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-11-05T17:20:00"}],
         "items": [{"bien": {"categoria": {"id": "%1$s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
         "cantidad": 8, "donacionOriginalId": "%2$s"},
        {"id": "6f1c2c1e-0000-4000-8000-000000000002", "descripcion": "Arroz",
         "estadoActual": "Entregada", "fechaRegistro": "2026-10-01T10:00:00",
         "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-10-20T10:00:00"}],
         "cantidad": 5, "donacionOriginalId": "%2$s"},
        {"id": "6f1c2c1e-0000-4000-8000-000000000003", "descripcion": "Lentejas secas",
         "estadoActual": "EnTraslado", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "EnTraslado", "timestamp": "2026-11-08T09:15:00"}],
         "items": [{"bien": {"categoria": {"id": "%1$s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
         "cantidad": 15, "donacionOriginalId": "%2$s"},
        {"id": "6f1c2c1e-0000-4000-8000-000000000004", "descripcion": "Leche en polvo",
         "estadoActual": "LISTA_PARA_ENTREGAR", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "ListaParaEntregar", "timestamp": "2026-11-06T10:00:00"}],
         "cantidad": 12, "donacionOriginalId": "%3$s"},
        {"id": "6f1c2c1e-0000-4000-8000-000000000005", "descripcion": "Aceite",
         "estadoActual": "Vencida", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "Vencida", "timestamp": "2026-11-07T08:00:00"}],
         "cantidad": 10, "donacionOriginalId": "%4$s"},
        {"id": "6f1c2c1e-0000-4000-8000-000000000006", "descripcion": "Silla",
         "estadoActual": "AsignacionRealizada", "fechaRegistro": "2026-11-02T10:00:00",
         "historial": [{"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-11-03T11:00:00"}],
         "cantidad": 1, "donacionOriginalId": "%5$s"},
        {"id": "6f1c2c1e-0000-4000-8000-000000000007", "descripcion": "Yerba",
         "estadoActual": "EntregaFallida", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "EntregaFallida", "timestamp": "2026-11-04T16:10:00"}],
         "cantidad": 2},
        {"id": "6f1c2c1e-0000-4000-8000-000000000008", "descripcion": "Mesa",
         "estadoActual": "EN_DEPOSITO", "fechaRegistro": "2026-11-01T10:00:00",
         "cantidad": 1, "donacionOriginalId": "%6$s"}
      ]
      """
          .formatted(ALIMENTOS, EMPRESA, PERSONA, BORRADA, SIN_DONANTE, SIN_NOMBRE);

  @Test
  void agrupaEnElOrdenDeLoQueHayQueHacerConQuienDonoYSoloLasRecibidasDelMes() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/donaciones-independientes?entidadId=" + ENTIDAD))
        .andRespond(withSuccess(OCHO, MediaType.APPLICATION_JSON));
    // El orden de las llamadas sigue el de la lista: problema, camino, salir y recibida.
    donaciones.expect(once(), requestTo(ORIGINAL + BORRADA)).andRespond(withResourceNotFound());
    // Una sola llamada por donación original, aunque dos asignadas salgan de la misma.
    donaciones
        .expect(once(), requestTo(ORIGINAL + EMPRESA))
        .andRespond(
            withSuccess(
                "{\"donante\": {\"persona\": {\"tipo\": \"JURIDICA\","
                    + " \"razonSocial\": \"Distribuidora Ejemplo SA\"}}}",
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(once(), requestTo(ORIGINAL + PERSONA))
        .andRespond(
            withSuccess(
                "{\"donante\": {\"persona\": {\"tipo\": \"HUMANA\", \"nombre\": \"Martina\","
                    + " \"apellido\": \"Ruiz\"}}}",
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(once(), requestTo(ORIGINAL + SIN_DONANTE))
        .andRespond(withSuccess("{\"donante\": null}", MediaType.APPLICATION_JSON));
    donaciones
        .expect(once(), requestTo(ORIGINAL + SIN_NOMBRE))
        .andRespond(
            withSuccess(
                "{\"donante\": {\"persona\": {\"nombre\": \" \"}}}", MediaType.APPLICATION_JSON));
    // "Hoy" es el 8 de noviembre de 2026, 18:30 en Buenos Aires.
    Clock hoy = Clock.fixed(Instant.parse("2026-11-08T21:30:00Z"), ZoneOffset.UTC);
    RecibidasRest adaptador =
        new RecibidasRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class), hoy);

    RecibidasVista vista = adaptador.recibidas(ENTIDAD);

    donaciones.verify();
    List<Recibida> lista = vista.asignadas();
    assertEquals(
        List.of("problema", "problema", "camino", SALIR, SALIR, SALIR, "recibida"),
        lista.stream().map(Recibida::grupo).toList());
    assertEquals("noviembre", vista.mes());
    assertEquals("8 de noviembre de 2026, 18:30", vista.actualizado());
    assertNull(vista.novedades());

    Recibida aceite = lista.get(0);
    assertEquals("Venció en el depósito el 7 de noviembre.", aceite.paso());
    assertEquals("un donante", aceite.dono());

    assertEquals("El camión no pudo llegar el 4 de noviembre.", lista.get(1).paso());

    Recibida lentejas = lista.get(2);
    assertEquals("Salió del depósito el 8 de noviembre a las 9:15.", lentejas.paso());
    assertEquals("Distribuidora Ejemplo SA", lentejas.dono());
    assertEquals("cat-alimentos", lentejas.categoria());
    assertEquals("Alimentos", lentejas.categoriaNombre());
    assertEquals("15 kg", lentejas.cuanto());
    assertNull(lentejas.pedido());

    Recibida leche = lista.get(3);
    assertEquals("Lista para entregar desde el 6 de noviembre. Espera el camión.", leche.paso());
    assertEquals("Martina Ruiz", leche.dono());

    Recibida silla = lista.get(4);
    assertEquals("Asignada el 3 de noviembre. Espera el camión.", silla.paso());
    assertEquals("un donante", silla.dono());
    assertNull(silla.categoria());
    assertNull(silla.categoriaNombre());
    assertEquals("1", silla.cuanto());

    Recibida mesa = lista.get(5);
    assertEquals("Asignada el 1 de noviembre. Espera el camión.", mesa.paso());
    assertEquals("un donante", mesa.dono());

    Recibida fideos = lista.get(6);
    assertEquals("Confirmaste la recepción el 5 de noviembre.", fideos.paso());
    assertEquals("5", fideos.dia());
    assertEquals("2026-11-05", fideos.fecha());
    assertEquals("Fideos secos", fideos.nombre());
  }
}
