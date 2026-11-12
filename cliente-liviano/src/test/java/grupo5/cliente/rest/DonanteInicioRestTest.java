package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteTableroVista;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
    apis.expect(
            requestTo(
                "http://logistica/api/entregas/seguimiento/donantes/" + DONANTE + "?size=100"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    apis.expect(
            requestTo("http://donaciones/donaciones-independientes?size=100&donanteId=" + DONANTE))
        .andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
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
    apis.expect(
            requestTo(
                "http://logistica/api/entregas/seguimiento/donantes/" + DONANTE + "?size=100"))
        .andRespond(withServerError());
    apis.expect(
            requestTo("http://donaciones/donaciones-independientes?size=100&donanteId=" + DONANTE))
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

  private static final String DONACIONES = "http://donaciones/donaciones-independientes?donanteId=";
  private static final String NOTIFICACIONES =
      "http://notificaciones/api/notificaciones/persona/" + DONANTE + "/no-leidas/conteo";

  /** Una página con su total en X-Total-Count. */
  private void pagina(String consulta, String cuerpo, long total) {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", String.valueOf(total));
    apis.expect(requestTo(DONACIONES + DONANTE + consulta))
        .andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON).headers(cabeceras));
  }

  @Test
  void elAnioDeLaPersonaPideTresPorMesYLasCifrasComoTotales() {
    // De enero a septiembre, una página de 3 por mes; septiembre trae una.
    for (int mes = 1; mes <= 9; mes++) {
      String inicio = "2026-%02d-01".formatted(mes);
      String fin = YearMonth.of(2026, mes).atEndOfMonth().toString();
      String cuerpo =
          mes == 9
              ? """
                [{"id": "6f1c2c1e-0000-4000-8000-000000000002", "descripcion": "Ropa de abrigo",
                  "estadoActual": "Entregada", "fechaRegistro": "2026-09-01T10:00:00",
                  "cantidad": 12, "entidadBeneficiariaId": "%s",
                  "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-09-09T18:05:00"}]}]
                """
                  .formatted(ENTIDAD)
              : "[]";
      pagina("&desde=" + inicio + "&hasta=" + fin + "&page=0&size=3", cuerpo, mes == 9 ? 1 : 0);
    }
    // Octubre: 7 registradas, llegan las 3 más recientes (las de la constante TRES, para no
    // repetirlas; el total manda).
    pagina("&desde=2026-10-01&hasta=2026-10-31&page=0&size=3", TRES, 7);
    apis.expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + ENTIDAD + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\"}]",
                MediaType.APPLICATION_JSON));
    String anio = "&desde=2026-01-01&hasta=2026-12-31";
    pagina(anio + "&estado=ENTREGADA&page=0&size=1", "[]", 4);
    pagina(anio + "&estado=EN_TRASLADO&estado=EN_PUERTA&page=0&size=1", "[]", 1);
    pagina(
        anio
            + "&estado=EN_DEPOSITO&estado=ASIGNACION_REALIZADA&estado=LISTA_PARA_ENTREGAR"
            + "&page=0&size=1",
        "[]",
        3);
    apis.expect(requestTo(NOTIFICACIONES))
        .andRespond(withSuccess("{\"noLeidas\": 2}", MediaType.APPLICATION_JSON));

    DonanteAnioVista vista = adaptador.anio(DONANTE);

    apis.verify();
    assertEquals(2026, vista.anio());
    assertEquals(10, vista.mesHoy());
    assertEquals(10, vista.meses().size());
    assertEquals(8, vista.total());
    assertEquals(4, vista.entregadas());
    assertEquals(1, vista.enCamino());
    assertEquals(3, vista.enDeposito());
    assertTrue(vista.algunaVez());
    DonanteAnioVista.Mes octubre = vista.meses().get(9);
    assertEquals("octubre", octubre.nombre());
    assertEquals("2026-10", octubre.clave());
    assertEquals(7, octubre.total());
    assertEquals(3, octubre.primeras().size());
    DonanteAnioVista.Mes septiembre = vista.meses().get(8);
    assertEquals(1, septiembre.total());
    assertEquals("Ropa de abrigo", septiembre.primeras().getFirst().titulo());
    assertEquals("Comedor Barrio Ejemplo", septiembre.primeras().getFirst().destino());
    assertEquals(0, vista.meses().getFirst().total());
    assertEquals(2, vista.sinLeer());
    assertEquals(3, vista.avisos().size());
    assertEquals("15 de octubre", vista.hoy());
  }

  @Test
  void unaCuentaSinDonacionesNoPideCifrasYDiceSiDonoAlgunaVez() {
    for (int mes = 1; mes <= 10; mes++) {
      String inicio = "2026-%02d-01".formatted(mes);
      String fin = YearMonth.of(2026, mes).atEndOfMonth().toString();
      pagina("&desde=" + inicio + "&hasta=" + fin + "&page=0&size=3", "[]", 0);
    }
    pagina("&page=0&size=1", "[]", 0);
    apis.expect(requestTo(NOTIFICACIONES)).andRespond(withServerError());

    DonanteAnioVista vista = adaptador.anio(DONANTE);

    apis.verify();
    assertEquals(0, vista.total());
    assertFalse(vista.algunaVez());
    assertEquals(0, vista.sinLeer());
  }

  private static final String TENER = "&estado=ENTREGA_FALLIDA&estado=VENCIDA";
  private static final String CAMINO =
      "&estado=EN_TRASLADO&estado=EN_PUERTA&estado=ASIGNACION_REALIZADA"
          + "&estado=LISTA_PARA_ENTREGAR";
  private static final String LLEGO = "&estado=ENTREGADA";

  /** Una asignada que espera el camión, hacia la entidad de ejemplo. */
  private static final String ASIGNADA =
      """
      [{"id": "6f1c2c1e-0000-4000-8000-000000000004", "descripcion": "Leche en polvo",
        "estadoActual": "AsignacionRealizada", "fechaRegistro": "2026-10-12T10:00:00",
        "cantidad": 6, "entidadBeneficiariaId": "%s",
        "historial": [{"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-10-13T09:00:00"}]}]
      """
          .formatted(ENTIDAD);

  @Test
  void elTableroPideUnaPaginaDeTresPorColumnaYLasQueEsperanDestinoComoCifra() {
    pagina(TENER + "&page=0&size=3", "[]", 0);
    pagina("&estado=EN_DEPOSITO&page=0&size=1", "[]", 2);
    pagina(CAMINO + "&page=1&size=3", ASIGNADA, 5);
    pagina(LLEGO + "&page=0&size=3", "[]", 48);
    apis.expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + ENTIDAD + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\"}]",
                MediaType.APPLICATION_JSON));
    apis.expect(
            requestTo(
                "http://logistica/api/entregas/seguimiento/donantes/" + DONANTE + "?size=100"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    DonanteTableroVista tablero = adaptador.tablero(DONANTE, 1, 2, 1);

    apis.verify();
    assertEquals(0, tablero.tener().total());
    assertEquals(2, tablero.esperanDestino());
    assertEquals(5, tablero.camino().total());
    assertEquals(2, tablero.camino().pagina());
    assertEquals(2, tablero.camino().paginas());
    assertEquals("Leche en polvo", tablero.camino().filas().getFirst().titulo());
    assertEquals("Comedor Barrio Ejemplo", tablero.camino().filas().getFirst().destino());
    assertEquals(48, tablero.llego().total());
    assertEquals(16, tablero.llego().paginas());
    assertEquals(5, tablero.enCurso());
  }

  @Test
  void unaPaginaQueSePasaMuestraLaUltimaYUnaColumnaQueFallaQuedaSola() {
    pagina(TENER + "&page=0&size=3", "[]", 0);
    pagina("&estado=EN_DEPOSITO&page=0&size=1", "[]", 0);
    apis.expect(requestTo(DONACIONES + DONANTE + CAMINO + "&page=0&size=3"))
        .andRespond(withServerError());
    pagina(LLEGO + "&page=98&size=3", "[]", 48);
    pagina(LLEGO + "&page=15&size=3", "[]", 48);

    DonanteTableroVista tablero = adaptador.tablero(DONANTE, 1, 1, 99);

    apis.verify();
    assertNull(tablero.camino());
    assertNull(tablero.enCurso());
    assertEquals(16, tablero.llego().pagina());
    assertEquals(0, tablero.tener().total());
  }
}
