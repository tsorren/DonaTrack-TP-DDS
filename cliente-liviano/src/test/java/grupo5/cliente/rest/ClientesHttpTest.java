package grupo5.cliente.rest;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

/**
 * Las cuatro APIs llevan el token de quien tiene la sesión ({@code tokenDelUsuario}, fuera de la
 * maqueta) y, sin ese interceptor (la maqueta), ninguna autorización. Sin ingreso, el interceptor
 * real no pone cabecera: lo prueba {@code IntegracionConfigTest}.
 */
class ClientesHttpTest {

  private static final String URL = "http://api";
  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000011");

  /** Una llamada de lectura de cada API: su ruta, una respuesta y cómo hacerla. */
  private record Caso(
      String ruta,
      String cuerpo,
      BiConsumer<ClientesHttp, ObjectProvider<ClientHttpRequestInterceptor>> llamada) {}

  private static final Caso INCENTIVOS =
      new Caso(
          "/api/incentivos/ranking/historial",
          "[]",
          (http, token) -> http.incentivosApi(URL, token).historial());
  private static final Caso DONACIONES =
      new Caso(
          "/api/donaciones/contadores",
          "{}",
          (http, token) -> http.donacionesApi(URL, token).contadores());
  private static final Caso LOGISTICA =
      new Caso("/api/entregas/mapa", "[]", (http, token) -> http.logisticaApi(URL, token).mapa());
  private static final Caso NOTIFICACIONES =
      new Caso(
          "/api/notificaciones/persona/" + ID + "/no-leidas/conteo",
          "{\"noLeidas\": 0}",
          (http, token) -> http.notificacionesApi(URL, token).sinLeer(ID));

  @Test
  void incentivosLlevaElTokenDelUsuario() {
    conToken(INCENTIVOS);
  }

  @Test
  void donacionesLlevaElTokenDelUsuario() {
    conToken(DONACIONES);
  }

  @Test
  void logisticaLlevaElTokenDelUsuario() {
    conToken(LOGISTICA);
  }

  @Test
  void notificacionesLlevaElTokenDelUsuario() {
    conToken(NOTIFICACIONES);
  }

  @Test
  void enLaMaquetaNingunaApiLlevaAutorizacion() {
    for (Caso caso : new Caso[] {INCENTIVOS, DONACIONES, LOGISTICA, NOTIFICACIONES}) {
      llamar(caso, interceptores(), headerDoesNotExist(HttpHeaders.AUTHORIZATION));
    }
  }

  private static void conToken(Caso caso) {
    ClientHttpRequestInterceptor usuario =
        (pedido, cuerpo, ejecucion) -> {
          pedido.getHeaders().setBearerAuth("token-del-usuario");
          return ejecucion.execute(pedido, cuerpo);
        };
    llamar(
        caso,
        interceptores(usuario),
        header(HttpHeaders.AUTHORIZATION, "Bearer token-del-usuario"));
  }

  /** Hace la llamada de un caso y verifica que el servidor de mentira recibió esa autorización. */
  private static void llamar(
      Caso caso,
      ObjectProvider<ClientHttpRequestInterceptor> interceptores,
      RequestMatcher autorizacion) {
    MockRestServiceServer[] servidor = new MockRestServiceServer[1];
    // El constructor de cada API se ata a un servidor de mentira que espera esa llamada.
    ClientesHttp http =
        new ClientesHttp(
            () -> {
              RestClient.Builder builder = RestClient.builder();
              servidor[0] = MockRestServiceServer.bindTo(builder).build();
              servidor[0]
                  .expect(requestTo(URL + caso.ruta()))
                  .andExpect(autorizacion)
                  .andRespond(withSuccess(caso.cuerpo(), MediaType.APPLICATION_JSON));
              return builder;
            });

    caso.llamada().accept(http, interceptores);

    servidor[0].verify();
  }

  /** Lo que Spring inyecta como {@code tokenDelUsuario}: nada en la maqueta, uno fuera de ella. */
  private static ObjectProvider<ClientHttpRequestInterceptor> interceptores(
      ClientHttpRequestInterceptor... beans) {
    DefaultListableBeanFactory fabrica = new DefaultListableBeanFactory();
    for (ClientHttpRequestInterceptor bean : beans) {
      fabrica.registerSingleton("tokenDelUsuario", bean);
    }
    return fabrica.getBeanProvider(ClientHttpRequestInterceptor.class);
  }
}
