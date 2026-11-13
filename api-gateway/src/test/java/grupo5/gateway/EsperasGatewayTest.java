package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * R11-04: el gateway no espera sin límite a un servicio lento. Pasado el límite de lectura contesta
 * 504 (el cliente liviano lo lleva a su página de falla); la subida de un archivo tiene su propio
 * límite, más largo. El servicio falso tarda más que la lectura y menos que la subida.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"donatrack.gateway.limites.lectura=300ms", "donatrack.gateway.limites.subida=5s"})
class EsperasGatewayTest {

  private static final long DEMORA_MS = 900;
  private static final HttpServer LENTO = servicioLento();

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int puerto;

  @DynamicPropertySource
  static void urls(DynamicPropertyRegistry registry) {
    String url = "http://localhost:" + LENTO.getAddress().getPort();
    registry.add("donatrack.gateway.servicios.donaciones", () -> url);
    registry.add("donatrack.gateway.servicios.notificaciones", () -> url);
    registry.add("donatrack.gateway.servicios.incentivos", () -> url);
    registry.add("donatrack.gateway.servicios.logistica", () -> url);
  }

  @AfterAll
  static void apagar() {
    LENTO.stop(0);
  }

  @Test
  void unServicioQueTardaMasQueElLimiteDeLecturaDa504() throws Exception {
    long inicio = System.nanoTime();

    HttpResponse<String> respuesta = pedir("GET", "/api/donaciones/contadores");

    assertThat(respuesta.statusCode()).isEqualTo(504);
    assertThat(Duration.ofNanos(System.nanoTime() - inicio))
        .isLessThan(Duration.ofMillis(DEMORA_MS));
  }

  @Test
  void laSubidaDeUnArchivoTieneUnLimiteMasLargo() throws Exception {
    assertThat(pedir("POST", "/api/donantes/archivos").statusCode()).isEqualTo(200);
    assertThat(pedir("POST", "/api/entregas/7/fotos").statusCode()).isEqualTo(200);
  }

  private HttpResponse<String> pedir(String metodo, String ruta) throws Exception {
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .method(
                metodo,
                "POST".equals(metodo) ? BodyPublishers.ofString("x") : BodyPublishers.noBody())
            .build();
    return http.send(pedido, HttpResponse.BodyHandlers.ofString());
  }

  private static HttpServer servicioLento() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/",
          intercambio -> {
            intercambio.getRequestBody().readAllBytes();
            try {
              Thread.sleep(DEMORA_MS);
            } catch (InterruptedException interrumpido) {
              Thread.currentThread().interrupt();
            }
            byte[] cuerpo = "ok".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(200, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
