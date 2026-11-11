package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
  private static final String SALIR = "salir";

  /**
   * Ocho asignadas, desordenadas: una entregada este mes y otra el anterior (no se muestra), una en
   * traslado, una lista para entregar, una vencida, una que no pudo llegar, una asignada sin
   * categoría y una en depósito. Quién donó llega en {@code quienDono}: una empresa, una persona,
   * una que ocultó su nombre (null), una sin {@code quienDono} y una con el nombre en blanco. La
   * donación original no se pide: la entidad no la puede leer.
   */
  private static final String OCHO =
      """
      [
        {"id": "6f1c2c1e-0000-4000-8000-000000000001", "descripcion": "Fideos secos",
         "estadoActual": "Entregada", "fechaRegistro": "2026-10-01T10:00:00",
         "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-11-05T17:20:00"}],
         "items": [{"bien": {"categoria": {"id": "%1$s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
         "cantidad": 8, "quienDono": {"tipo": "JURIDICA", "nombreVisible": "Distribuidora Ejemplo SA"}},
        {"id": "6f1c2c1e-0000-4000-8000-000000000002", "descripcion": "Arroz",
         "estadoActual": "Entregada", "fechaRegistro": "2026-10-01T10:00:00",
         "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-10-20T10:00:00"}],
         "cantidad": 5, "quienDono": {"tipo": "JURIDICA", "nombreVisible": "Distribuidora Ejemplo SA"}},
        {"id": "6f1c2c1e-0000-4000-8000-000000000003", "descripcion": "Lentejas secas",
         "estadoActual": "EnTraslado", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "EnTraslado", "timestamp": "2026-11-08T09:15:00"}],
         "items": [{"bien": {"categoria": {"id": "%1$s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
         "cantidad": 15, "quienDono": {"tipo": "JURIDICA", "nombreVisible": "Distribuidora Ejemplo SA"}},
        {"id": "6f1c2c1e-0000-4000-8000-000000000004", "descripcion": "Leche en polvo",
         "estadoActual": "LISTA_PARA_ENTREGAR", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "ListaParaEntregar", "timestamp": "2026-11-06T10:00:00"}],
         "cantidad": 12, "quienDono": {"tipo": "HUMANA", "nombreVisible": "Martina Ruiz"}},
        {"id": "6f1c2c1e-0000-4000-8000-000000000005", "descripcion": "Aceite",
         "estadoActual": "Vencida", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "Vencida", "timestamp": "2026-11-07T08:00:00"}],
         "cantidad": 10, "quienDono": {"tipo": "HUMANA", "nombreVisible": null}},
        {"id": "6f1c2c1e-0000-4000-8000-000000000006", "descripcion": "Silla",
         "estadoActual": "AsignacionRealizada", "fechaRegistro": "2026-11-02T10:00:00",
         "historial": [{"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-11-03T11:00:00"}],
         "cantidad": 1, "quienDono": null},
        {"id": "6f1c2c1e-0000-4000-8000-000000000007", "descripcion": "Yerba",
         "estadoActual": "EntregaFallida", "fechaRegistro": "2026-11-01T10:00:00",
         "historial": [{"estadoNuevo": "EntregaFallida", "timestamp": "2026-11-04T16:10:00"}],
         "cantidad": 2},
        {"id": "6f1c2c1e-0000-4000-8000-000000000008", "descripcion": "Mesa",
         "estadoActual": "EN_DEPOSITO", "fechaRegistro": "2026-11-01T10:00:00",
         "cantidad": 1, "quienDono": {"tipo": "HUMANA", "nombreVisible": " "}}
      ]
      """
          .formatted(ALIMENTOS);

  @Test
  void agrupaEnElOrdenDeLoQueHayQueHacerConQuienDonoYSoloLasRecibidasDelMes() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/donaciones-independientes?entidadId=" + ENTIDAD))
        .andRespond(withSuccess(OCHO, MediaType.APPLICATION_JSON));
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
    assertEquals("salió del depósito el 8 de noviembre", lentejas.breve());
    assertNull(lentejas.llego());

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
    assertNull(fideos.breve());
    assertEquals("Fideos secos", fideos.nombre());
  }

  @Test
  void laQueLlegoALaPuertaEsperaLaConfirmacionPrimeroConLaHoraDeLlegada() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/donaciones-independientes?entidadId=" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                [{"id": "6f1c2c1e-0000-4000-8000-000000000003", "descripcion": "Arroz",
                  "estadoActual": "EN_TRASLADO", "fechaRegistro": "2026-11-01T10:00:00",
                  "historial": [{"estadoNuevo": "EnTraslado", "timestamp": "2026-11-08T09:15:00"}],
                  "cantidad": 5},
                 {"id": "6f1c2c1e-0000-4000-8000-000000000009", "descripcion": "Lentejas secas",
                  "estadoActual": "EN_PUERTA", "fechaRegistro": "2026-11-01T10:00:00",
                  "historial": [{"estadoNuevo": "EnTraslado", "timestamp": "2026-11-08T09:15:00"},
                                {"estadoNuevo": "EN_PUERTA", "timestamp": "2026-11-08T18:20:00"}],
                  "cantidad": 15}]
                """,
                MediaType.APPLICATION_JSON));
    Clock hoy = Clock.fixed(Instant.parse("2026-11-08T21:30:00Z"), ZoneOffset.UTC);
    RecibidasRest adaptador =
        new RecibidasRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class), hoy);

    List<Recibida> lista = adaptador.recibidas(ENTIDAD).asignadas();

    assertEquals(List.of("puerta", "camino"), lista.stream().map(Recibida::grupo).toList());
    Recibida lentejas = lista.getFirst();
    assertEquals(
        "Llegó a tu puerta el 8 de noviembre a las 18:20. Espera tu confirmación.",
        lentejas.paso());
    assertEquals("Llegó el 8 de noviembre, 18:20.", lentejas.llego());
    assertEquals("llegó a tu puerta el 8 de noviembre", lentejas.breve());
    assertNull(lista.get(1).llego());
  }
}
