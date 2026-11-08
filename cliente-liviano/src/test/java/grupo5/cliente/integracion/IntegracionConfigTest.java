package grupo5.cliente.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

import com.sun.net.httpserver.HttpServer;
import grupo5.cliente.puertos.PersonaNueva.Humana;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/** El sitio llama a Donaciones y a Keycloak con el token técnico, pedido una sola vez. */
class IntegracionConfigTest {

  private static final UUID PERSONA = UUID.randomUUID();

  private final List<String> autorizaciones = new CopyOnWriteArrayList<>();
  private final AtomicInteger pedidosDeToken = new AtomicInteger();
  private HttpServer servidor;
  private String url;
  private DonacionesRest donaciones;

  @BeforeEach
  void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    servidor.createContext(
        "/token",
        intercambio -> {
          pedidosDeToken.incrementAndGet();
          responder(
              intercambio,
              200,
              "{\"access_token\":\"token-tecnico\",\"token_type\":\"Bearer\",\"expires_in\":300}");
        });
    servidor.createContext(
        "/api/personas",
        intercambio -> {
          autorizaciones.add(intercambio.getRequestHeaders().getFirst("Authorization"));
          responder(
              intercambio, 201, "{\"id\":\"" + PERSONA + "\",\"tipo\":\"HUMANA\",\"extra\":1}");
        });
    servidor.start();
    url = "http://localhost:" + servidor.getAddress().getPort();
    var clientes =
        new InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId("servicio")
                .clientId("cliente-liviano")
                .clientSecret("secreto")
                .authorizationGrantType(CLIENT_CREDENTIALS)
                .tokenUri(url + "/token")
                .build());
    var config = new IntegracionConfig();
    var interceptor = config.tokenDeServicio(clientes);
    donaciones = new DonacionesRest(config.donaciones(interceptor, url));
  }

  @AfterEach
  void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(
      com.sun.net.httpserver.HttpExchange intercambio, int estado, String cuerpo)
      throws IOException {
    byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
    intercambio.getResponseHeaders().add("Content-Type", "application/json");
    intercambio.sendResponseHeaders(estado, bytes.length);
    intercambio.getResponseBody().write(bytes);
    intercambio.close();
  }

  @Test
  void laLlamadaLlevaElTokenDeServicioYSePideUnaSolaVez() {
    UUID primera = donaciones.crear(new Humana("Ana", "Pérez", "ana@ejemplo.test"));
    UUID segunda = donaciones.crear(new Humana("Ana", "Pérez", "ana@ejemplo.test"));

    assertThat(primera).isEqualTo(PERSONA);
    assertThat(segunda).isEqualTo(PERSONA);
    assertThat(autorizaciones).containsExactly("Bearer token-tecnico", "Bearer token-tecnico");
    assertThat(pedidosDeToken.get()).isEqualTo(1);
  }
}
