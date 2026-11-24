package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Las llamadas a las APIs llevan la IP del navegador en {@code X-Forwarded-For}: el rate limiting
 * del gateway (E7, ADR 20261115) cuenta por ella a quien no tiene sesión. El contacto ya la manda
 * por su cuenta (HB-21) y no se pisa.
 */
class ClientesHttpIpDelNavegadorTest {

  private static final String URL = "http://api";
  private static final String NAVEGADOR = "198.51.100.20";

  private RestClient.Builder builder;
  private MockRestServiceServer servidor;

  @BeforeEach
  void armar() {
    builder = RestClient.builder();
    servidor = MockRestServiceServer.bindTo(builder).build();
  }

  @AfterEach
  void limpiar() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void cadaLlamadaLlevaLaIpDelNavegador() {
    navegadorEn(NAVEGADOR);
    servidor
        .expect(requestTo(URL + "/api/donaciones/contadores"))
        .andExpect(header(ClientesHttp.X_FORWARDED_FOR, NAVEGADOR))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    ClientesHttp.cliente(builder, URL, DonacionesApi.class).contadores();

    servidor.verify();
  }

  @Test
  void elContactoConservaLaIpQueYaTrae() {
    navegadorEn(NAVEGADOR);
    servidor
        .expect(requestTo(URL + "/api/contacto"))
        .andExpect(
            pedido ->
                assertThat(pedido.getHeaders().getValuesAsList(ClientesHttp.X_FORWARDED_FOR))
                    .containsExactly("203.0.113.7"))
        .andRespond(withSuccess("{\"numero\": 1}", MediaType.APPLICATION_JSON));

    ClientesHttp.cliente(builder, URL, NotificacionesApi.class)
        .contacto(
            "203.0.113.7",
            new NotificacionesApi.MensajeDeContacto(
                "SITIO", "Hola.", null, "ana@ejemplo.test", UUID.randomUUID()));

    servidor.verify();
  }

  @Test
  void fueraDeUnPedidoDelNavegadorNoLaPone() {
    servidor
        .expect(requestTo(URL + "/api/donaciones/contadores"))
        .andExpect(headerDoesNotExist(ClientesHttp.X_FORWARDED_FOR))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    ClientesHttp.cliente(builder, URL, DonacionesApi.class).contadores();

    servidor.verify();
  }

  private static void navegadorEn(String ip) {
    MockHttpServletRequest pedido = new MockHttpServletRequest();
    pedido.setRemoteAddr(ip);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(pedido));
  }
}
