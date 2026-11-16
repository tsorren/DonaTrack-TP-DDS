package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.RankingVista;
import grupo5.cliente.vistas.RankingVista.Puesto;
import grupo5.cliente.web.TraceIdFiltro;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** El adaptador REST del ranking contra respuestas de ejemplo de Incentivos (plan E6, §F1). */
class RankingRestTest {

  private static final String RANKING = "http://incentivos/api/incentivos/ranking/";

  /** R11-43: la primera página de 12 meses. */
  private static final String HISTORIAL = RANKING + "historial?page=0&size=12";

  /** Dos meses desordenados, uno sin fecha de cálculo, con campos que el cliente no usa. */
  private static final String DOS_MESES =
      """
      [
        {"periodo": "2026-08", "calculadoEn": null,
         "entradas": [{"posicion": 1, "donanteId": "6f1c2c1e-0000-4000-8000-000000000001",
                       "nombreDonante": "Juan Ejemplo", "misionesCompletadas": 6}],
         "podio": []},
        {"periodo": "2026-09", "calculadoEn": "2026-10-01T02:59:00Z",
         "entradas": [
           {"posicion": 2, "donanteId": "6f1c2c1e-0000-4000-8000-000000000001",
            "nombreDonante": "Juan Ejemplo", "misionesCompletadas": 4},
           {"posicion": 1, "donanteId": "6f1c2c1e-0000-4000-8000-000000000002",
            "nombreDonante": "Ana Ejemplo", "misionesCompletadas": 5}],
         "podio": []}
      ]
      """;

  private MockRestServiceServer incentivos;
  private RankingRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    incentivos = MockRestServiceServer.bindTo(builder).build();
    Clock noviembre = Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneOffset.UTC);
    adaptador =
        new RankingRest(
            ClientesHttp.cliente(builder, "http://incentivos", IncentivosApi.class), noviembre);
  }

  @AfterEach
  void limpiar() {
    MDC.remove(TraceIdFiltro.CLAVE);
  }

  @Test
  void ordenaLosMesesDelMasRecienteYEscribeFechasEnCastellano() {
    incentivos
        .expect(requestTo(HISTORIAL))
        .andRespond(withSuccess(DOS_MESES, MediaType.APPLICATION_JSON));

    RankingVista ranking = adaptador.ranking();

    assertEquals(
        List.of("2026-09", "2026-08"),
        ranking.meses().stream().map(RankingVista.Mes::clave).toList());
    RankingVista.Mes septiembre = ranking.meses().get(0);
    assertEquals("Septiembre de 2026", septiembre.nombre());
    assertEquals("30 de septiembre de 2026, 23:59", septiembre.calculado());
    assertEquals(
        List.of(new Puesto("Ana Ejemplo", 5), new Puesto("Juan Ejemplo", 4)), septiembre.puestos());
    assertNull(ranking.meses().get(1).calculado());
    assertEquals("noviembre", ranking.proximo());
    incentivos.verify();
  }

  @Test
  void sinRankingsDevuelveLaListaVacia() {
    incentivos
        .expect(requestTo(HISTORIAL))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertTrue(adaptador.ranking().meses().isEmpty());
  }

  @Test
  void mandaElTraceIdDelPedido() {
    MDC.put(TraceIdFiltro.CLAVE, "0123456789abcdef0123456789abcdef");
    incentivos
        .expect(requestTo(HISTORIAL))
        .andExpect(header(TraceIdFiltro.CABECERA, "0123456789abcdef0123456789abcdef"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertTrue(adaptador.ranking().meses().isEmpty());
    incentivos.verify();
  }

  @Test
  void unaFallaDeIncentivosLlegaComoErrorDelClienteHttp() {
    incentivos.expect(requestTo(HISTORIAL)).andRespond(withServerError());

    assertThrows(RestClientException.class, adaptador::ranking);
  }

  // ===================== R11-43: de a una página de meses =====================

  @Test
  void otraPaginaConElTotalYElPrimeroDeEsaPaginaElegido() {
    incentivos
        .expect(requestTo(RANKING + "historial?page=1&size=12"))
        .andRespond(
            withSuccess(DOS_MESES, MediaType.APPLICATION_JSON).header("X-Total-Count", "14"));

    RankingVista ranking = adaptador.ranking(null, 2);

    incentivos.verify();
    assertEquals(14, ranking.total());
    assertEquals(2, ranking.pagina());
    assertEquals(2, ranking.paginas());
    assertEquals("2026-09", ranking.elegido().clave());
  }

  @Test
  void elMesPedidoDeLaPaginaNoSePideAparte() {
    incentivos
        .expect(requestTo(HISTORIAL))
        .andRespond(withSuccess(DOS_MESES, MediaType.APPLICATION_JSON));

    assertEquals("2026-08", adaptador.ranking("2026-08", 1).elegido().clave());
    incentivos.verify();
  }

  @Test
  void elMesPedidoDeOtraPaginaSePideSoloYUnoQueNoSeCalculoDaElPrimero() {
    incentivos
        .expect(requestTo(HISTORIAL))
        .andRespond(withSuccess(DOS_MESES, MediaType.APPLICATION_JSON));
    incentivos
        .expect(requestTo(RANKING + "2025-01"))
        .andRespond(
            withSuccess(
                """
                {"periodo": "2025-01", "calculadoEn": null,
                 "entradas": [{"posicion": 1, "nombreDonante": "Ana Ejemplo", "misionesCompletadas": 3}]}
                """,
                MediaType.APPLICATION_JSON));
    incentivos
        .expect(requestTo(HISTORIAL))
        .andRespond(withSuccess(DOS_MESES, MediaType.APPLICATION_JSON));
    incentivos.expect(requestTo(RANKING + "2020-01")).andRespond(withStatus(HttpStatus.NOT_FOUND));

    RankingVista.Mes enero = adaptador.ranking("2025-01", 1).elegido();
    RankingVista.Mes sinCalcular = adaptador.ranking("2020-01", 1).elegido();

    incentivos.verify();
    assertEquals("Enero de 2025", enero.nombre());
    assertEquals(List.of(new Puesto("Ana Ejemplo", 3)), enero.puestos());
    assertEquals("2026-09", sinCalcular.clave());
  }
}
