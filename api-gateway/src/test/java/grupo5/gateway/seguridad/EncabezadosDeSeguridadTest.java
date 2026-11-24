package grupo5.gateway.seguridad;

import static grupo5.gateway.seguridad.ServicioQueMandaEncabezados.tieneLosSieteUnaSolaVez;
import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
 * Los siete encabezados de seguridad, una sola vez, en las cuatro clases de respuesta del gateway
 * con la seguridad encendida: ruta enrutada con token, ruta pública sin token, 401 y 404.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "donatrack.seguridad.habilitada=true")
@Import(EncabezadosDeSeguridadTest.Tokens.class)
class EncabezadosDeSeguridadTest {

  private static final HttpServer SERVICIO = ServicioQueMandaEncabezados.iniciar();

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int puerto;

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
    ServicioQueMandaEncabezados.apuntarLosServicios(registry, SERVICIO);
  }

  @AfterAll
  static void apagarServicio() {
    SERVICIO.stop(0);
  }

  @ParameterizedTest(name = "{0} {1} -> {2}")
  @CsvSource({
    "enrutada con token, /api/donaciones, 200, true",
    "pública sin token, /api/entidades/publicas, 200, false",
    "protegida sin token, /api/donaciones, 401, false",
    "fuera de la tabla, /api/inexistente, 404, true"
  })
  void cadaRespuestaLlevaLosSieteEncabezadosUnaSolaVez(
      String caso, String ruta, int estado, boolean conToken) throws Exception {
    HttpRequest.Builder pedido =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
    if (conToken) {
      pedido.header("Authorization", "Bearer valido");
    }

    HttpResponse<String> respuesta =
        http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());

    assertThat(respuesta.statusCode()).as(caso).isEqualTo(estado);
    tieneLosSieteUnaSolaVez(respuesta);
  }
}
