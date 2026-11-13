package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.DonanteNotificacionesVista;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Aviso;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** La bandeja del donante contra respuestas de ejemplo de Notificaciones, de a una página. */
class DonanteNotificacionesRestTest {

  private static final UUID PERSONA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String UNA = "6f1c2c1e-0000-4000-8000-0000000000b1";
  private static final String OTRA = "6f1c2c1e-0000-4000-8000-0000000000b2";
  private static final String BANDEJA =
      "http://notificaciones/api/notificaciones/persona/" + PERSONA;
  private static final String CONTEO = BANDEJA + "/no-leidas/conteo";

  private static final String DONACION = "6f1c2c1e-0000-4000-8000-0000000000d1";

  /**
   * En el orden del servicio (la más nueva primero), una leída; textos como los arma
   * Notificaciones. La asignada trae su tipo y su donación (HB-09); la otra es de antes y no trae
   * ninguno.
   */
  private static final String DOS =
      """
      [
        {"id": "%2$s", "mensaje": "¡Felicitaciones! Has ascendido de la categoría A a B.",
         "estado": "ENVIADA", "fechaCreacion": "2026-10-15T16:02:00",
         "fechaLectura": "2026-10-15T16:10:00"},
        {"id": "%1$s", "mensaje": "Tu donación de Fideos fue asignada a Comedor Barrio Ejemplo",
         "estado": "ENVIADA", "fechaCreacion": "2026-10-14T12:15:30", "fechaLectura": null,
         "tipoEvento": "DONACION_ASIGNADA", "referenciaId": "%3$s"}
      ]
      """
          .formatted(UNA, OTRA, DONACION);

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

  private static HttpHeaders total(long total) {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", String.valueOf(total));
    return cabeceras;
  }

  @Test
  void pideLaPrimeraPaginaDe20YArmaCadaAvisoConSuTituloCategoriaYVinculo() {
    apis.expect(requestTo(BANDEJA + "?page=0&size=20"))
        .andRespond(withSuccess(DOS, MediaType.APPLICATION_JSON).headers(total(57)));
    apis.expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": 4}", MediaType.APPLICATION_JSON));

    DonanteNotificacionesVista vista = adaptador.bandeja(PERSONA, ConsultaBandeja.primera());

    apis.verify();
    assertEquals("2026-10-15T16:25", vista.ahora());
    assertEquals(57, vista.pagina().total());
    assertEquals(1, vista.pagina().pagina());
    assertEquals(3, vista.pagina().paginas());
    assertEquals(4, vista.sinLeer());
    Aviso ascenso = vista.pagina().filas().get(0);
    assertEquals(OTRA, ascenso.id());
    assertEquals("Novedad", ascenso.titulo());
    assertEquals("otras", ascenso.tema());
    assertEquals("/donante-donaciones", ascenso.destino());
    assertEquals("Ver Mis donaciones", ascenso.vinculo());
    assertTrue(ascenso.leida());
    Aviso asignada = vista.pagina().filas().get(1);
    assertEquals("2026-10-14T12:15", asignada.fecha());
    assertEquals("Donación asignada", asignada.titulo());
    assertEquals("asignadas", asignada.tema());
    assertEquals("/donante-donaciones-detalle?donacion=" + DONACION, asignada.destino());
    assertEquals("Ver la donación", asignada.vinculo());
    assertFalse(asignada.leida());
  }

  @Test
  void losFiltrosDeLaDireccionVanAlServicio() {
    apis.expect(
            requestTo(
                BANDEJA
                    + "?leida=false&q=lentejas&tipo=ENTREGA_FALLIDA&tipo=DONACION_VENCIDA"
                    + "&orden=antiguas&page=1&size=20"))
        .andRespond(withSuccess(DOS, MediaType.APPLICATION_JSON).headers(total(22)));
    apis.expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": 4}", MediaType.APPLICATION_JSON));

    adaptador.bandeja(PERSONA, new ConsultaBandeja(true, " lentejas ", "problemas", true, 2));

    assertDoesNotThrow(() -> apis.verify());
  }

  @Test
  void otrasNovedadesExcluyeLosTiposDeLasDemasCategorias() {
    apis.expect(
            requestTo(
                BANDEJA
                    + "?excluirTipo=DONACION_EN_CAMINO&excluirTipo=DONACION_RECIBIDA"
                    + "&excluirTipo=DONACION_ASIGNADA&excluirTipo=ENTREGA_FALLIDA"
                    + "&excluirTipo=DONACION_VENCIDA&page=0&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(0)));
    apis.expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": 0}", MediaType.APPLICATION_JSON));

    DonanteNotificacionesVista vista =
        adaptador.bandeja(PERSONA, new ConsultaBandeja(false, "", "otras", false, 1));

    assertDoesNotThrow(() -> apis.verify());
    assertTrue(vista.pagina().filas().isEmpty());
  }

  @Test
  void unaPaginaQueYaNoExisteSePideComoLaUltima() {
    apis.expect(requestTo(BANDEJA + "?page=98&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(45)));
    apis.expect(requestTo(BANDEJA + "?page=2&size=20"))
        .andRespond(withSuccess(DOS, MediaType.APPLICATION_JSON).headers(total(45)));
    apis.expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": 1}", MediaType.APPLICATION_JSON));

    DonanteNotificacionesVista vista =
        adaptador.bandeja(PERSONA, new ConsultaBandeja(false, "", "", false, 99));

    assertDoesNotThrow(() -> apis.verify());
    assertEquals(3, vista.pagina().pagina());
  }

  @Test
  void laCategoriaYElTituloSalenDelTipoDeEventoYSinTipoSonOtrasNovedades() {
    assertEquals("problemas", DonanteNotificacionesRest.tema("ENTREGA_FALLIDA"));
    assertEquals("problemas", DonanteNotificacionesRest.tema("DONACION_VENCIDA"));
    assertEquals("entregadas", DonanteNotificacionesRest.tema("DONACION_RECIBIDA"));
    assertEquals("camino", DonanteNotificacionesRest.tema("DONACION_EN_CAMINO"));
    assertEquals("asignadas", DonanteNotificacionesRest.tema("DONACION_ASIGNADA"));
    assertEquals("otras", DonanteNotificacionesRest.tema("MISION_CUMPLIDA"));
    assertEquals("otras", DonanteNotificacionesRest.tema("UN_TIPO_QUE_NO_CONOCEMOS"));
    assertEquals("otras", DonanteNotificacionesRest.tema(null));
    assertEquals("Donación en camino", DonanteNotificacionesRest.titulo("DONACION_EN_CAMINO"));
    assertEquals("Misión cumplida", DonanteNotificacionesRest.titulo("MISION_CUMPLIDA"));
    assertEquals("Novedad", DonanteNotificacionesRest.titulo("UN_TIPO_QUE_NO_CONOCEMOS"));
    assertEquals("Novedad", DonanteNotificacionesRest.titulo(null));
  }

  @Test
  void marcarNoPideLaBandejaYSalteaLasAjenasOInexistentes() {
    String ajena = "6f1c2c1e-0000-4000-8000-0000000000ff";
    apis.expect(requestTo("http://notificaciones/api/notificaciones/" + UNA + "/leida?leida=true"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    apis.expect(
            requestTo("http://notificaciones/api/notificaciones/" + ajena + "/leida?leida=true"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));
    apis.expect(requestTo("http://notificaciones/api/notificaciones/" + OTRA + "/leida?leida=true"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    adaptador.marcar(PERSONA, List.of(UNA, ajena, "x", OTRA, UNA), true);

    assertDoesNotThrow(() -> apis.verify());
  }

  @Test
  void marcarTodasUsaLaDeLaPersona() {
    apis.expect(requestTo(BANDEJA + "/marcar-todas-leidas"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withSuccess());

    adaptador.marcarTodas(PERSONA);

    assertDoesNotThrow(() -> apis.verify());
  }
}
