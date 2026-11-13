package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El gateway con la seguridad encendida (ADR-E6-02, etapa 2): sin token solo pasa la allowlist
 * pública y lo demás da 401; con token, el pedido llega al servicio con el {@code Authorization}
 * intacto, porque el servicio vuelve a validarlo (segunda capa). Cuatro servicios falsos responden
 * con su nombre, la ruta y el {@code Authorization} que recibieron.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "donatrack.seguridad.habilitada=true")
@Import(SeguridadGatewayTest.Tokens.class)
class SeguridadGatewayTest {

  private static final String AUTH_RECIBIDO = "X-Auth-Recibido";
  private static final String BEARER = "Bearer valido";
  private static final Map<String, HttpServer> SERVICIOS =
      Map.of(
          "donaciones", servicioFalso("donaciones"),
          "notificaciones", servicioFalso("notificaciones"),
          "incentivos", servicioFalso("incentivos"),
          "logistica", servicioFalso("logistica"));

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int puerto;

  @Autowired private GatewayProperties props;

  @TestConfiguration
  static class Tokens {

    @Bean
    JwtDecoder decodificador() {
      return valor -> {
        if (!"valido".equals(valor)) {
          throw new BadJwtException("token desconocido");
        }
        return Jwt.withTokenValue("t")
            .header("alg", "none")
            .claim("roles", List.of("DONANTE"))
            .build();
      };
    }
  }

  @DynamicPropertySource
  static void urlsDeServicios(DynamicPropertyRegistry registry) {
    SERVICIOS.forEach(
        (nombre, server) ->
            registry.add(
                "donatrack.gateway.servicios." + nombre,
                () -> "http://localhost:" + server.getAddress().getPort()));
  }

  @AfterAll
  static void apagarServicios() {
    SERVICIOS.values().forEach(s -> s.stop(0));
  }

  @ParameterizedTest(name = "{0} sin token -> 401")
  @CsvSource({
    "/api/donaciones",
    "/api/donaciones/42",
    "/api/donantes/7",
    "/api/notificaciones/persona/7/no-leidas/conteo",
    "/api/incentivos/ranking/ultimo",
    "/api/entregas",
    "/api/rutas/5",
    "/api/logistica/planificaciones/9",
    "/api/contacto/4821"
  })
  void sinTokenUnaRutaProtegidaDa401(String ruta) throws Exception {
    assertThat(pedir(ruta, null).statusCode()).isEqualTo(401);
  }

  @Test
  void elErrorDe401TieneElFormatoUnificado() throws Exception {
    HttpResponse<String> respuesta = pedir("/api/donaciones", null);

    assertThat(respuesta.statusCode()).isEqualTo(401);
    assertThat(respuesta.body()).contains("ERR-CSR-401");
  }

  @Test
  void unTokenInvalidoDa401() throws Exception {
    assertThat(pedir("/api/donaciones", "Bearer trucho").statusCode()).isEqualTo(401);
  }

  @ParameterizedTest(name = "{0} con token -> {1}")
  @CsvSource({
    "/api/donaciones, donaciones",
    "/api/notificaciones/persona/7, notificaciones",
    "/api/incentivos/ranking/ultimo, incentivos",
    "/api/entregas, logistica"
  })
  void conTokenElPedidoLlegaAlServicioConElBearerIntacto(String ruta, String servicio)
      throws Exception {
    HttpResponse<String> respuesta = pedir(ruta, BEARER);

    assertThat(respuesta.statusCode()).isEqualTo(200);
    assertThat(respuesta.body()).isEqualTo(servicio + " " + ruta);
    assertThat(respuesta.headers().firstValue(AUTH_RECIBIDO)).contains(BEARER);
  }

  @Test
  void cadaRutaPublicaPasaSinTokenYSinTokenLlegaAlServicio() throws Exception {
    assertThat(props.rutasPublicas()).isNotEmpty();
    for (String rutaPublica : props.rutasPublicas()) {
      String[] partes = rutaPublica.split(" ");
      HttpResponse<String> respuesta = pedir(partes[0], partes[1], null);

      assertThat(respuesta.statusCode()).as(rutaPublica).isEqualTo(200);
      assertThat(respuesta.headers().firstValue(AUTH_RECIBIDO)).as(rutaPublica).isEmpty();
    }
  }

  @Test
  void unaRutaPublicaNoAbreLasQueCuelganDelMismoPrefijo() throws Exception {
    assertThat(pedir("/api/entidades/publicas", null).statusCode()).isEqualTo(200);
    assertThat(pedir("/api/entidades", null).statusCode()).isEqualTo(401);
    assertThat(pedir("/api/entidades/7", null).statusCode()).isEqualTo(401);
    assertThat(pedir("/api/necesidades/publicas", null).statusCode()).isEqualTo(200);
    assertThat(pedir("/api/necesidades/7", null).statusCode()).isEqualTo(401);
    assertThat(pedir("/api/donaciones/contadores", null).statusCode()).isEqualTo(200);
    assertThat(pedir("/api/donaciones/contadores/otra", null).statusCode()).isEqualTo(401);
    assertThat(pedir("POST", "/api/contacto", null).statusCode()).isEqualTo(200);
    assertThat(pedir("/api/contacto/4821", null).statusCode()).isEqualTo(401);
    assertThat(pedir("POST", "/api/contacto/4821/respuesta", null).statusCode()).isEqualTo(401);
  }

  @Test
  void unaRutaFueraDeLaTablaPideTokenYConTokenDa404() throws Exception {
    assertThat(pedir("/api/inexistente", null).statusCode()).isEqualTo(401);
    assertThat(pedir("/api/inexistente", BEARER).statusCode()).isEqualTo(404);
    assertThat(pedir("/donaciones-independientes", BEARER).statusCode()).isEqualTo(404);
  }

  @Test
  void elHealthcheckNoPideToken() throws Exception {
    assertThat(pedir("/actuator/health", null).statusCode()).isEqualTo(200);
  }

  private HttpResponse<String> pedir(String ruta, String authorization)
      throws IOException, InterruptedException {
    return pedir("GET", ruta, authorization);
  }

  private HttpResponse<String> pedir(String metodo, String ruta, String authorization)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .method(metodo, HttpRequest.BodyPublishers.noBody());
    if (authorization != null) {
      builder.header("Authorization", authorization);
    }
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static HttpServer servicioFalso(String nombre) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization != null) {
              exchange.getResponseHeaders().add(AUTH_RECIBIDO, authorization);
            }
            byte[] cuerpo =
                (nombre + " " + exchange.getRequestURI().getPath())
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, cuerpo.length);
            exchange.getResponseBody().write(cuerpo);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
