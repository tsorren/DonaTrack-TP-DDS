package grupo5.gateway.seguridad;

import static grupo5.gateway.seguridad.ServicioQueMandaEncabezados.tieneLosSeisSinCacheControl;
import static grupo5.gateway.seguridad.ServicioQueMandaEncabezados.tieneLosSieteUnaSolaVez;
import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Con la seguridad apagada (el valor por defecto) la cadena de {@code common-lib} es otra; los
 * encabezados salen igual.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EncabezadosConSeguridadApagadaTest {

  private static final HttpServer SERVICIO = ServicioQueMandaEncabezados.iniciar();

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int puerto;

  @DynamicPropertySource
  static void urlsDeServicios(DynamicPropertyRegistry registry) {
    ServicioQueMandaEncabezados.apuntarLosServicios(registry, SERVICIO);
  }

  @AfterAll
  static void apagarServicio() {
    SERVICIO.stop(0);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({"/api/donaciones, 200", "/api/inexistente, 404"})
  void cadaRespuestaLlevaLosSieteEncabezadosUnaSolaVez(String ruta, int estado) throws Exception {
    HttpResponse<String> respuesta =
        http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).build(),
            HttpResponse.BodyHandlers.ofString());

    assertThat(respuesta.statusCode()).isEqualTo(estado);
    tieneLosSieteUnaSolaVez(respuesta);
  }

  @Test
  void fueraDeApiSalenLosOtrosSeisYElCacheControlNoEsElDelGateway() throws Exception {
    HttpResponse<String> respuesta =
        http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/actuator/health"))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertThat(respuesta.statusCode()).isEqualTo(200);
    tieneLosSeisSinCacheControl(respuesta);
  }
}
