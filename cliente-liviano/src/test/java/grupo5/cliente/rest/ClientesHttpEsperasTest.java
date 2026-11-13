package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * R11-04: una API lenta no deja la pantalla colgada. Una lectura que pasa el límite falla con una
 * excepción de red (que {@code ErroresHttp} lleva a la página de falla con su código de soporte), y
 * la subida de un archivo, que es más lenta por naturaleza, tiene su propio límite, más largo.
 */
class ClientesHttpEsperasTest {

  /** Lo que tarda en responder el servidor de mentira. */
  private static final long DEMORA_MS = 700;

  private static final Duration LECTURA = Duration.ofMillis(250);
  private static final Duration SUBIDA = Duration.ofSeconds(5);

  private static HttpServer servidor;
  private static String url;

  @BeforeAll
  static void levantarServidorLento() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    servidor.createContext(
        "/",
        intercambio -> {
          intercambio.getRequestBody().readAllBytes();
          try {
            Thread.sleep(DEMORA_MS);
          } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
          }
          String json =
              intercambio.getRequestURI().getPath().endsWith("/archivos")
                  ? "{\"id\":\"00000000-0000-4000-8000-000000000011\"}"
                  : "{}";
          byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
          intercambio.getResponseHeaders().add("Content-Type", "application/json");
          intercambio.sendResponseHeaders(200, cuerpo.length);
          intercambio.getResponseBody().write(cuerpo);
          intercambio.close();
        });
    servidor.start();
    url = "http://localhost:" + servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static DonacionesApi donaciones() {
    ClientesHttp.Esperas esperas = new ClientesHttp.Esperas(Duration.ofSeconds(2), LECTURA, SUBIDA);
    return ClientesHttp.cliente(
        RestClient.builder().requestFactory(esperas.fabrica()), url, DonacionesApi.class);
  }

  @Test
  void unaLecturaQueSuperaElLimiteFallaEnVezDeQuedarColgada() {
    DonacionesApi api = donaciones();

    long inicio = System.nanoTime();
    assertThatThrownBy(api::contadores).isInstanceOf(ResourceAccessException.class);

    assertThat(Duration.ofNanos(System.nanoTime() - inicio))
        .isLessThan(Duration.ofMillis(DEMORA_MS));
  }

  @Test
  void laSubidaDeUnArchivoAguantaMasQueUnaLecturaComun() {
    MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
    partes.add("archivo", "dni,nombre\n");

    assertThat(donaciones().importarDonantes(partes).id()).isNotNull();
  }

  @ParameterizedTest(name = "{0} {1} -> subida: {2}")
  @CsvSource({
    "POST, /api/donantes/archivos, true",
    "POST, /api/entregas/7/fotos, true",
    "GET, /api/donantes/archivos/7, false",
    "GET, /api/donantes, false",
    "POST, /api/donaciones, false"
  })
  void soloLasSubidasUsanElLimiteLargo(String metodo, String ruta, boolean esSubida) {
    URI uri = URI.create("http://api" + ruta);

    assertThat(ClientesHttp.Esperas.esSubida(uri, HttpMethod.valueOf(metodo))).isEqualTo(esSubida);
  }
}
