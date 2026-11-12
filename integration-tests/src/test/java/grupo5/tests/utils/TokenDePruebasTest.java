package grupo5.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** El token de pruebas contra un Keycloak falso (un {@link HttpServer} del JDK). */
@Tag("smoke")
class TokenDePruebasTest {

  /** Un reloj que se mueve a mano. */
  private static final class RelojManual extends Clock {
    private Instant ahora = Instant.parse("2026-11-11T12:00:00Z");

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
      return this;
    }

    @Override
    public Instant instant() {
      return ahora;
    }

    void avanzar(long segundos) {
      ahora = ahora.plusSeconds(segundos);
    }
  }

  private HttpServer keycloak;
  private String url;
  private final AtomicInteger pedidos = new AtomicInteger();
  private final AtomicReference<String> cuerpo = new AtomicReference<>();
  private final AtomicReference<String> contentType = new AtomicReference<>();
  private final AtomicReference<String> respuesta =
      new AtomicReference<>("{\"access_token\":\"tok-1\",\"expires_in\":900}");
  private final AtomicInteger estado = new AtomicInteger(200);
  private final RelojManual reloj = new RelojManual();

  @BeforeEach
  void levantarKeycloak() throws IOException {
    keycloak = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    keycloak.createContext(
        "/token",
        exchange -> {
          pedidos.incrementAndGet();
          cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
          byte[] bytes = respuesta.get().getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(estado.get(), bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    keycloak.start();
    url = "http://localhost:" + keycloak.getAddress().getPort() + "/token";
  }

  @AfterEach
  void apagarKeycloak() {
    keycloak.stop(0);
    System.clearProperty("pruebas.client-secret");
  }

  private TokenDePruebas token() {
    return new TokenDePruebas(url, "donatrack-pruebas", "s3cr&t o+=/", reloj);
  }

  @Test
  void pideElTokenPorClientCredentialsConElSecretoCodificado() {
    assertEquals("tok-1", token().token());

    assertEquals(1, pedidos.get());
    assertEquals(
        "grant_type=client_credentials&client_id=donatrack-pruebas"
            + "&client_secret=s3cr%26t+o%2B%3D%2F",
        cuerpo.get());
    assertEquals("application/x-www-form-urlencoded", contentType.get());
  }

  @Test
  void dentroDeLaVigenciaUsaElTokenGuardado() {
    TokenDePruebas token = token();
    token.token();
    reloj.avanzar(800);

    assertEquals("tok-1", token.token());
    assertEquals(1, pedidos.get());
  }

  @Test
  void aMenosDe30SegundosDeVencerRenuevaAntesDeQueFalle() {
    TokenDePruebas token = token();
    token.token();
    respuesta.set("{\"access_token\":\"tok-2\",\"expires_in\":900}");
    reloj.avanzar(875);

    assertEquals("tok-2", token.token());
    assertEquals(2, pedidos.get());
  }

  @Test
  void sinExpiresInDuraSesentaSegundosMenosElMargen() {
    respuesta.set("{\"access_token\":\"tok-3\"}");
    TokenDePruebas token = token();
    token.token();
    reloj.avanzar(20);
    token.token();
    assertEquals(1, pedidos.get());

    reloj.avanzar(15);
    token.token();
    assertEquals(2, pedidos.get());
  }

  @Test
  void unRechazoCortaSinMostrarElSecretoNiLaRespuestaDeKeycloak() {
    estado.set(401);
    respuesta.set("{\"error\":\"unauthorized_client\",\"error_description\":\"secreto s3cr\"}");
    TokenDePruebas token = token();

    IllegalStateException error = assertThrows(IllegalStateException.class, token::token);

    assertTrue(error.getMessage().contains("401"));
    assertTrue(error.getMessage().contains("PRUEBAS_CLIENT_SECRET"));
    assertFalse(error.getMessage().contains("s3cr"));
    assertFalse(error.getMessage().contains("unauthorized_client"));
  }

  @Test
  void unaRespuestaSinAccessTokenCorta() {
    respuesta.set("{\"otra\":\"cosa\"}");
    TokenDePruebas token = token();

    IllegalStateException error = assertThrows(IllegalStateException.class, token::token);

    assertTrue(error.getMessage().contains("access_token"));
  }

  @Test
  void conKeycloakCaidoCortaDiciendoAQuienLePidio() {
    keycloak.stop(0);
    TokenDePruebas token = token();

    IllegalStateException error = assertThrows(IllegalStateException.class, token::token);

    assertTrue(error.getMessage().contains(url));
  }

  @Test
  void sinElSecretoNoHayTokenYLaSeguridadApagadaSigueComoSiempre() {
    System.clearProperty("pruebas.client-secret");
    assertEquals(
        System.getenv("PRUEBAS_CLIENT_SECRET") == null
            || System.getenv("PRUEBAS_CLIENT_SECRET").isBlank(),
        TokenDePruebas.delEntorno().isEmpty());

    System.setProperty("pruebas.client-secret", "   ");
    assertEquals(
        System.getenv("PRUEBAS_CLIENT_SECRET") == null
            || System.getenv("PRUEBAS_CLIENT_SECRET").isBlank(),
        TokenDePruebas.delEntorno().isEmpty());

    System.setProperty("pruebas.client-secret", "algo");
    assertTrue(TokenDePruebas.delEntorno().isPresent());
  }

  @Test
  void conVariosHilosALaVezSePideUnaSolaVez() throws InterruptedException {
    TokenDePruebas token = token();
    AtomicInteger iguales = new AtomicInteger();
    List<Thread> hilos = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      Thread hilo =
          new Thread(
              () -> {
                if ("tok-1".equals(token.token())) {
                  iguales.incrementAndGet();
                }
              });
      hilos.add(hilo);
      hilo.start();
    }
    for (Thread hilo : hilos) {
      hilo.join();
    }

    assertEquals(20, iguales.get());
    assertEquals(1, pedidos.get());
  }
}
