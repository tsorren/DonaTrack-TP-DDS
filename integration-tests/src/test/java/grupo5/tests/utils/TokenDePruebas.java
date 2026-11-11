package grupo5.tests.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token de la cuenta de servicio de pruebas ({@code donatrack-pruebas}, client credentials, rol
 * ADMIN) para llamar a los servicios cuando la seguridad está encendida (SPEC-06 E2-07). Se guarda
 * hasta poco antes de vencer. Sin {@code PRUEBAS_CLIENT_SECRET} no hay token y los pedidos salen
 * como siempre, que es lo que pasa con la seguridad apagada.
 */
public final class TokenDePruebas {

  static final String TOKEN_URL_POR_DEFECTO =
      "http://localhost:8180/realms/donatrack/protocol/openid-connect/token";
  static final String CLIENTE_POR_DEFECTO = "donatrack-pruebas";

  private static final Duration MARGEN = Duration.ofSeconds(30);
  private static final long VIDA_POR_DEFECTO_SEGUNDOS = 60;
  private static final Pattern ACCESS_TOKEN =
      Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
  private static final Pattern EXPIRES_IN = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");

  private final String tokenUrl;
  private final String clientId;
  private final String secreto;
  private final Clock reloj;
  private final HttpClient http = HttpClient.newHttpClient();

  private String token;
  private Instant vence = Instant.EPOCH;

  public TokenDePruebas(String tokenUrl, String clientId, String secreto) {
    this(tokenUrl, clientId, secreto, Clock.systemUTC());
  }

  TokenDePruebas(String tokenUrl, String clientId, String secreto, Clock reloj) {
    this.tokenUrl = tokenUrl;
    this.clientId = clientId;
    this.secreto = secreto;
    this.reloj = reloj;
  }

  /** Arma el cliente con {@code -D} o con variables de entorno; vacío si no hay secreto. */
  public static Optional<TokenDePruebas> delEntorno() {
    String secreto = valor("pruebas.client-secret", "PRUEBAS_CLIENT_SECRET", "");
    if (secreto.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new TokenDePruebas(
            valor("keycloak.token-url", "KEYCLOAK_TOKEN_URL", TOKEN_URL_POR_DEFECTO),
            valor("pruebas.client-id", "PRUEBAS_CLIENT_ID", CLIENTE_POR_DEFECTO),
            secreto));
  }

  /** El token vigente; pide uno nuevo si falta o está por vencer. */
  public synchronized String token() {
    if (token == null || !reloj.instant().isBefore(vence.minus(MARGEN))) {
      renovar();
    }
    return token;
  }

  private void renovar() {
    String cuerpo =
        "grant_type=client_credentials&client_id="
            + codificar(clientId)
            + "&client_secret="
            + codificar(secreto);
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build();
    HttpResponse<String> respuesta;
    try {
      respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo pedir el token de pruebas a " + tokenUrl, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Se interrumpió el pedido del token de pruebas", e);
    }
    // Nunca se imprime el secreto: ni en el mensaje ni en la respuesta.
    if (respuesta.statusCode() != 200) {
      throw new IllegalStateException(
          "Keycloak respondió "
              + respuesta.statusCode()
              + " al pedir el token de pruebas: ¿PRUEBAS_CLIENT_SECRET es el del realm?");
    }
    Matcher acceso = ACCESS_TOKEN.matcher(respuesta.body());
    if (!acceso.find()) {
      throw new IllegalStateException("La respuesta de Keycloak no trae access_token");
    }
    Matcher vida = EXPIRES_IN.matcher(respuesta.body());
    long segundos = vida.find() ? Long.parseLong(vida.group(1)) : VIDA_POR_DEFECTO_SEGUNDOS;
    token = acceso.group(1);
    vence = reloj.instant().plusSeconds(segundos);
  }

  private static String codificar(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }

  private static String valor(String propiedad, String variable, String porDefecto) {
    String valor = System.getProperty(propiedad);
    if (valor == null || valor.isBlank()) {
      valor = System.getenv(variable);
    }
    return valor == null || valor.isBlank() ? porDefecto : valor;
  }
}
