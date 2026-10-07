package grupo5.donaciones.infrastructure.logistica;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import grupo5.donaciones.services.logistica.ErrorContratoProveedorException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProveedorLogisticaHttpTest {

  private static final String BASE_URL = "http://externo.test";
  private static final UUID ENVIO_ID = UUID.randomUUID();
  private static final String TRACE_ID = "trace-http-001";
  private static final UUID DONACION_ID = UUID.randomUUID();
  private static final UUID BENEFICIARIA_ID = UUID.randomUUID();
  private static final DatosEntregaLogistica DATOS =
      new DatosEntregaLogistica(
          DONACION_ID,
          BENEFICIARIA_ID,
          new DestinoEventoDTO(
              "Av. Siempreviva", 742, null, null, "1000", "CABA", "Buenos Aires", "Argentina"),
          12.5,
          0.3,
          LocalDateTime.of(2026, 10, 7, 12, 0));

  private MockRestServiceServer servidor;
  private ProveedorLogisticaHttp proveedor;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    servidor = MockRestServiceServer.bindTo(builder).build();
    proveedor = new ProveedorLogisticaHttp("externo", builder.build());
  }

  @Test
  void id_devuelveElIdDelProveedor() {
    assertEquals("externo", proveedor.id());
  }

  @Test
  void enviar_traduceAlContratoRestDelProveedorYPropagaElTraceId() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Trace-Id", TRACE_ID))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.idDonacion").value(DONACION_ID.toString()))
        .andExpect(jsonPath("$.idBeneficiaria").value(BENEFICIARIA_ID.toString()))
        .andExpect(jsonPath("$.destino.calle").value("Av. Siempreviva"))
        .andExpect(jsonPath("$.destino.altura").value(742))
        .andExpect(jsonPath("$.pesoTotalKG").value(12.5))
        .andExpect(jsonPath("$.volumenTotalM3").value(0.3))
        .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED));

    assertDoesNotThrow(() -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));

    servidor.verify();
  }

  @Test
  void enviar_conflictoEsExito_elProveedorYaTeniaLaDonacion() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));

    assertDoesNotThrow(() -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @Test
  void enviar_503EsRechazado_seguroQueNoSeProceso() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

    assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @ParameterizedTest
  @ValueSource(ints = {500, 502, 504})
  void enviar_erroresDelServidorSonInciertos_puedeHaberseProcesado(int estado) {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withStatus(org.springframework.http.HttpStatusCode.valueOf(estado)));

    assertThrows(EnvioInciertoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 404, 422})
  void enviar_otros4xxSonErrorDeContrato_noSeReintentaNiSeReenvia(int estado) {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withStatus(org.springframework.http.HttpStatusCode.valueOf(estado)));

    assertThrows(
        ErrorContratoProveedorException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @Test
  void enviar_unaRedireccionNoEsExito() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withStatus(org.springframework.http.HttpStatus.FOUND));

    assertThrows(
        ErrorContratoProveedorException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @Test
  void enviar_conexionRechazadaEsRechazado() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withException(new ConnectException("Connection refused")));

    assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @Test
  void enviar_hostInexistenteEsRechazado() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withException(new UnknownHostException("externo.test")));

    assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @Test
  void enviar_timeoutDeConexionEsRechazado() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withException(new HttpConnectTimeoutException("connect timed out")));

    assertThrows(EnvioRechazadoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }

  @Test
  void enviar_timeoutDeLecturaEsIncierto_elProveedorPudoRecibirlo() {
    servidor
        .expect(requestTo(BASE_URL + "/api/entregas"))
        .andRespond(withException(new SocketTimeoutException("Read timed out")));

    assertThrows(EnvioInciertoException.class, () -> proveedor.enviar(ENVIO_ID, DATOS, TRACE_ID));
  }
}
