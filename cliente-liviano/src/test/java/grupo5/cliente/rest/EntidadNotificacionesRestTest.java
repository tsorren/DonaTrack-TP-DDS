package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Novedad;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** La bandeja de la entidad contra respuestas de Notificaciones, de a una página (R11-14). */
class EntidadNotificacionesRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String UNA = "6f1c2c1e-0000-4000-8000-0000000000a1";
  private static final String OTRA = "6f1c2c1e-0000-4000-8000-0000000000a2";
  private static final String BASE = "http://notificaciones/api/notificaciones/";
  private static final String PRIMERA = BASE + "persona/" + ENTIDAD + "?page=0&size=20";
  private static final String CONTEO = BASE + "persona/" + ENTIDAD + "/no-leidas/conteo";

  private static final String DONACION = "6f1c2c1e-0000-4000-8000-0000000000d1";
  private static final String ENTREGA = "6f1c2c1e-0000-4000-8000-0000000000f1";
  private static final String SEGUIMIENTO =
      "http://logistica/api/entregas/seguimiento/beneficiarios/" + ENTIDAD;
  private static final String ASIGNADAS =
      "http://donaciones/donaciones-independientes?size=100&entidadId=" + ENTIDAD;

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
  // "Ahora" es el 8 de noviembre de 2026, 18:30 en Buenos Aires.
  private final EntidadNotificacionesRest adaptador =
      new EntidadNotificacionesRest(
          ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class),
          ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
          ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
          Clock.fixed(Instant.parse("2026-11-08T21:30:45Z"), ZoneOffset.UTC));

  private static HttpHeaders total(long total) {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", String.valueOf(total));
    return cabeceras;
  }

  private void conteo(long sinLeer) {
    servidor
        .expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": " + sinLeer + "}", MediaType.APPLICATION_JSON));
  }

  private EntidadNotificacionesVista primera() {
    return adaptador.bandeja(ENTIDAD, ConsultaBandeja.primera());
  }

  @Test
  void laBandejaTomaLaCategoriaYElTituloDelTipoYSinTipoVaAOtrasNovedades() {
    servidor
        .expect(requestTo(PRIMERA))
        .andRespond(
            withSuccess(
                    """
                    [{"id": "%s", "mensaje": "Se te ha asignado una donación.", "estado": "ENVIADA",
                      "fechaCreacion": "2026-11-08T09:15:30", "fechaLectura": null,
                      "tipoEvento": "DONACION_ASIGNADA", "referenciaId": "%s"},
                     {"id": "%s", "mensaje": "Un aviso de antes.", "estado": "ENVIADA",
                      "fechaCreacion": "2026-11-01T10:00:00", "fechaLectura": "2026-11-02T08:00:00"},
                     {"id": null, "mensaje": "Sin id", "fechaCreacion": "2026-11-01T10:00:00"}]
                    """
                        .formatted(UNA, DONACION, OTRA),
                    MediaType.APPLICATION_JSON)
                .headers(total(43)));
    conteo(5);

    EntidadNotificacionesVista vista = primera();

    servidor.verify();
    assertEquals("2026-11-08T18:30", vista.ahora());
    assertEquals("8 de noviembre de 2026, 18:30", vista.actualizado());
    assertEquals(
        List.of("", "camino", "asignadas", "recibidas", "problemas", "otras"),
        vista.temas().stream().map(t -> t.valor()).toList());
    assertEquals(43, vista.pagina().total());
    assertEquals(3, vista.pagina().paginas());
    assertEquals(5, vista.sinLeer());
    assertEquals(2, vista.pagina().filas().size());
    Novedad asignada = vista.pagina().filas().getFirst();
    assertEquals(UNA, asignada.id());
    assertEquals("2026-11-08T09:15", asignada.fecha());
    assertFalse(asignada.leida());
    assertEquals("asignadas", asignada.tema());
    assertEquals("Nueva donación asignada", asignada.titulo());
    assertEquals("Ver en Donaciones recibidas", asignada.vinculo());
    assertEquals("/entidad-recibidas?estado=por-salir", asignada.destino());
    assertNull(asignada.boton());
    Novedad vieja = vista.pagina().filas().get(1);
    assertTrue(vieja.leida());
    assertEquals("otras", vieja.tema());
    assertEquals("Novedad", vieja.titulo());
    assertEquals("/entidad-recibidas", vieja.destino());
  }

  @Test
  void cadaTipoDeEventoVaASuCategoriaYSuEnlace() {
    servidor
        .expect(requestTo(PRIMERA))
        .andRespond(
            withSuccess(
                    """
                    [{"id": "%1$s", "mensaje": "Llegó", "fechaCreacion": "2026-11-08T09:00:00",
                      "tipoEvento": "DONACION_RECIBIDA", "referenciaId": "%3$s"},
                     {"id": "%2$s", "mensaje": "No pudo llegar", "fechaCreacion": "2026-11-08T08:00:00",
                      "tipoEvento": "ENTREGA_FALLIDA"},
                     {"id": "%1$s", "mensaje": "Misión", "fechaCreacion": "2026-11-08T07:00:00",
                      "tipoEvento": "MISION_CUMPLIDA"}]
                    """
                        .formatted(UNA, OTRA, DONACION),
                    MediaType.APPLICATION_JSON)
                .headers(total(3)));
    conteo(0);

    EntidadNotificacionesVista vista = primera();

    servidor.verify();
    List<Novedad> filas = vista.pagina().filas();
    assertEquals(
        List.of("recibidas", "problemas", "otras"), filas.stream().map(Novedad::tema).toList());
    assertEquals(
        List.of("Entrega recibida", "La entrega tuvo un problema", "Novedad"),
        filas.stream().map(Novedad::titulo).toList());
    assertEquals("/entidad-recibidas?estado=recibidas", filas.get(0).destino());
    assertEquals("/entidad-recibidas", filas.get(1).destino());
  }

  @Test
  void losFiltrosVanAlServicioYOtrasExcluyeLosTiposDeLasDemas() {
    servidor
        .expect(
            requestTo(
                BASE
                    + "persona/"
                    + ENTIDAD
                    + "?leida=false&q=arroz&tipo=DONACION_RECIBIDA&orden=antiguas"
                    + "&page=1&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(25)));
    conteo(2);
    servidor
        .expect(
            requestTo(
                BASE
                    + "persona/"
                    + ENTIDAD
                    + "?excluirTipo=DONACION_EN_CAMINO&excluirTipo=DONACION_ASIGNADA"
                    + "&excluirTipo=DONACION_RECIBIDA&excluirTipo=ENTREGA_FALLIDA"
                    + "&excluirTipo=DONACION_VENCIDA&page=0&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(0)));
    conteo(2);

    adaptador.bandeja(ENTIDAD, new ConsultaBandeja(true, "arroz", "recibidas", true, 2));
    adaptador.bandeja(ENTIDAD, new ConsultaBandeja(false, "", "otras", false, 1));

    assertDoesNotThrow(() -> servidor.verify());
  }

  private String enCamino() {
    return """
        [{"id": "%s", "mensaje": "Tu donación salió del depósito.", "estado": "ENVIADA",
          "fechaCreacion": "2026-11-08T09:15:30", "fechaLectura": null,
          "tipoEvento": "DONACION_EN_CAMINO", "referenciaId": "%s"}]
        """
        .formatted(UNA, DONACION);
  }

  private void paginaEnCamino() {
    servidor
        .expect(requestTo(PRIMERA))
        .andRespond(withSuccess(enCamino(), MediaType.APPLICATION_JSON).headers(total(1)));
  }

  @Test
  void elAvisoDeEntregaEnCaminoLlevaAConfirmarRecepcionConLaEntregaDeLogistica() {
    paginaEnCamino();
    servidor
        .expect(requestTo(SEGUIMIENTO))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "EN_TRASLADO"},
                 {"idEntrega": "6f1c2c1e-0000-4000-8000-0000000000f2",
                  "idDonacion": "6f1c2c1e-0000-4000-8000-0000000000d2",
                  "estadoEntrega": "EN_TRASLADO"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(ASIGNADAS))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + DONACION + "\", \"cantidad\": 1}]", MediaType.APPLICATION_JSON));
    conteo(1);

    Novedad novedad = primera().pagina().filas().getFirst();

    servidor.verify();
    assertEquals("camino", novedad.tema());
    assertEquals("Entrega en camino", novedad.titulo());
    assertEquals("Confirmar recepción", novedad.boton());
    assertEquals("Tu donación salió del depósito.", novedad.que());
    assertEquals("/entidad-seguimiento-confirmar?entrega=" + ENTREGA, novedad.destino());
  }

  @Test
  void elAvisoDeEntregaQueLlegoALaPuertaTambienLlevaAConfirmarRecepcion() {
    paginaEnCamino();
    servidor
        .expect(requestTo(SEGUIMIENTO))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "EN_PUERTA"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(ASIGNADAS))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + DONACION + "\", \"cantidad\": 1}]", MediaType.APPLICATION_JSON));
    conteo(1);

    Novedad novedad = primera().pagina().filas().getFirst();

    servidor.verify();
    assertEquals("Confirmar recepción", novedad.boton());
    assertEquals("/entidad-seguimiento-confirmar?entrega=" + ENTREGA, novedad.destino());
  }

  @Test
  void siLaEntregaYaNoEspera_oLogisticaNoResponde_queda_elEnlaceSimpleSinBoton() {
    paginaEnCamino();
    servidor
        .expect(requestTo(SEGUIMIENTO))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "ENTREGADA"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
    conteo(1);
    paginaEnCamino();
    servidor.expect(requestTo(SEGUIMIENTO)).andRespond(withServerError());
    conteo(1);

    Novedad entregada = primera().pagina().filas().getFirst();
    Novedad sinLogistica = primera().pagina().filas().getFirst();

    servidor.verify();
    for (Novedad n : List.of(entregada, sinLogistica)) {
      assertNull(n.boton());
      assertEquals("/entidad-recibidas?estado=en-camino", n.destino());
    }
  }

  @Test
  void siLaDonacionNoEstaEntreLasAsignadasDeLaEntidad_oDonacionesNoResponde_noHayBoton() {
    String enTraslado =
        "[{\"idEntrega\": \""
            + ENTREGA
            + "\", \"idDonacion\": \""
            + DONACION
            + "\", \"estadoEntrega\": \"EN_TRASLADO\"}]";
    paginaEnCamino();
    servidor
        .expect(requestTo(SEGUIMIENTO))
        .andRespond(withSuccess(enTraslado, MediaType.APPLICATION_JSON));
    servidor.expect(requestTo(ASIGNADAS)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    conteo(1);
    paginaEnCamino();
    servidor
        .expect(requestTo(SEGUIMIENTO))
        .andRespond(withSuccess(enTraslado, MediaType.APPLICATION_JSON));
    servidor.expect(requestTo(ASIGNADAS)).andRespond(withServerError());
    conteo(1);

    Novedad ajena = primera().pagina().filas().getFirst();
    Novedad sinDonaciones = primera().pagina().filas().getFirst();

    servidor.verify();
    for (Novedad n : List.of(ajena, sinDonaciones)) {
      assertNull(n.boton());
      assertEquals("/entidad-recibidas?estado=en-camino", n.destino());
    }
  }

  @Test
  void sinAvisosDeEntregaEnCaminoNoLlamaALogisticaYSinConteoSonCero() {
    servidor
        .expect(requestTo(PRIMERA))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(0)));
    servidor.expect(requestTo(CONTEO)).andRespond(withServerError());

    EntidadNotificacionesVista vista = primera();

    servidor.verify();
    assertTrue(vista.pagina().filas().isEmpty());
    assertEquals(0, vista.sinLeer());
    assertEquals(6, vista.temas().size());
  }

  @Test
  void marcarLasElegidasSinNingunaTodasYDejarSinLeerSalteandoLasAjenas() {
    servidor
        .expect(requestTo(BASE + UNA + "/leida?leida=true"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(BASE + OTRA + "/leida?leida=true"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));
    servidor
        .expect(requestTo(BASE + "persona/" + ENTIDAD + "/marcar-todas-leidas"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess());
    servidor
        .expect(requestTo(BASE + OTRA + "/leida?leida=false"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    adaptador.marcarLeidas(ENTIDAD, List.of(UUID.fromString(UNA), UUID.fromString(OTRA)));
    adaptador.marcarLeidas(ENTIDAD, List.of());
    adaptador.dejarSinLeer(UUID.fromString(OTRA));

    servidor.verify();
  }
}
