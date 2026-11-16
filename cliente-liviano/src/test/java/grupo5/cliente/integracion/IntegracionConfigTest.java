package grupo5.cliente.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

import com.sun.net.httpserver.HttpServer;
import grupo5.cliente.puertos.PersonaNueva.Humana;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

/**
 * El sitio llama a Donaciones y a Keycloak con el token técnico, pedido una sola vez; como usuario,
 * con el de su ingreso, y sin ingreso, sin token.
 */
class IntegracionConfigTest {

  private static final UUID PERSONA = UUID.randomUUID();

  private final List<String> autorizaciones = new CopyOnWriteArrayList<>();
  private final AtomicInteger pedidosDeToken = new AtomicInteger();
  private HttpServer servidor;
  private String url;
  private PersonasRest donaciones;

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
    servidor.createContext(
        "/api/entidades",
        intercambio -> {
          autorizaciones.add(intercambio.getRequestHeaders().getFirst("Authorization"));
          responder(intercambio, 201, "{}");
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
    donaciones = new PersonasRest(config.donacionesComoServicio(interceptor, url));
  }

  @AfterEach
  void apagarServidor() {
    SecurityContextHolder.clearContext();
    servidor.stop(0);
  }

  @Test
  void conElTokenDelUsuarioLaLlamadaLlevaElDeSuIngreso() {
    ClientRegistration keycloak = registroKeycloak();
    var clientes = new InMemoryClientRegistrationRepository(keycloak);
    var ingresos = new InMemoryOAuth2AuthorizedClientService(clientes);
    var usuario = new DefaultOAuth2User(List.of(), Map.of("sub", "ana"), "sub");
    var sesion = new OAuth2AuthenticationToken(usuario, List.of(), "keycloak");
    Instant ahora = Instant.now();
    var token = new OAuth2AccessToken(BEARER, "token-usuario", ahora, ahora.plusSeconds(300));
    ingresos.saveAuthorizedClient(new OAuth2AuthorizedClient(keycloak, "ana", token), sesion);
    SecurityContextHolder.getContext().setAuthentication(sesion);
    var config = new IntegracionConfig();
    var interceptor = config.tokenDelUsuario(clientes, ingresos);

    new DonacionesRestComoUsuario(config.donacionesComoUsuario(interceptor, url))
        .registrarEntidad(PERSONA);

    assertThat(autorizaciones).containsExactly("Bearer token-usuario");
    assertThat(pedidosDeToken.get()).isZero();
  }

  @Test
  void sinIngresoLaLlamadaDelUsuarioVaSinToken() {
    var clientes = new InMemoryClientRegistrationRepository(registroKeycloak());
    var config = new IntegracionConfig();
    var interceptor =
        config.tokenDelUsuario(clientes, new InMemoryOAuth2AuthorizedClientService(clientes));

    new DonacionesRestComoUsuario(config.donacionesComoUsuario(interceptor, url))
        .registrarEntidad(PERSONA);

    assertThat(autorizaciones).hasSize(1).containsOnlyNulls();
    assertThat(pedidosDeToken.get()).isZero();
  }

  private ClientRegistration registroKeycloak() {
    return ClientRegistration.withRegistrationId("keycloak")
        .clientId("cliente-liviano")
        .authorizationGrantType(AUTHORIZATION_CODE)
        .redirectUri(url + "/{registrationId}")
        .authorizationUri(url + "/auth")
        .tokenUri(url + "/token")
        .build();
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
