package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Rate limiting del gateway (E7, requerimiento 3) contra un servicio falso que cuenta los pedidos
 * que le llegan. Cupos chicos para no hacer cientos de pedidos: 5 lecturas y 3 escrituras por IP y
 * por minuto. El test es el proxy confiable, así que cada caso usa su propia IP en {@code
 * X-Forwarded-For} y no comparte cubeta con los demás. Las cubetas van en memoria: Redis se prueba
 * en {@link CubetasEnRedisContraRedisTest}.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "donatrack.gateway.limite-de-solicitudes.lecturas-por-ip=5",
      "donatrack.gateway.limite-de-solicitudes.escrituras-por-ip=3",
      "donatrack.gateway.limite-de-solicitudes.proxies-confiables=127[.]0[.]0[.]1|0:0:0:0:0:0:0:1"
    })
@Import(LimiteDeSolicitudesGatewayTest.CubetasDelTest.class)
class LimiteDeSolicitudesGatewayTest {

  @TestConfiguration
  static class CubetasDelTest {
    @Bean
    @Primary
    LimiteDeSolicitudes.Almacen cubetasEnMemoria() {
      return new CubetasEnMemoria();
    }
  }

  private static final AtomicInteger RECIBIDOS = new AtomicInteger();
  private static final HttpServer SERVICIO = servicioQueCuenta();

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int puerto;

  @Autowired private MeterRegistry registro;

  @DynamicPropertySource
  static void urls(DynamicPropertyRegistry registry) {
    String url = "http://localhost:" + SERVICIO.getAddress().getPort();
    registry.add("donatrack.gateway.servicios.donaciones", () -> url);
    registry.add("donatrack.gateway.servicios.notificaciones", () -> url);
    registry.add("donatrack.gateway.servicios.incentivos", () -> url);
    registry.add("donatrack.gateway.servicios.logistica", () -> url);
  }

  @AfterAll
  static void apagar() {
    SERVICIO.stop(0);
  }

  @Test
  void pasadoElCupoDa429ConElErrorComunYElPedidoNoLlegaAlServicio() throws Exception {
    String ip = "203.0.113.10";
    int antes = RECIBIDOS.get();
    double rechazosAntes = rechazados("donaciones", "escritura");
    for (int i = 0; i < 3; i++) {
      assertThat(pedir("POST", "/api/donaciones", ip).statusCode()).isEqualTo(200);
    }

    HttpResponse<String> rechazado = pedir("POST", "/api/donaciones", ip);

    assertThat(rechazado.statusCode()).isEqualTo(429);
    assertThat(rechazado.body()).contains("\"code\":\"ERR-CSR-429\"").contains("\"traceId\"");
    assertThat(rechazado.headers().firstValue("Content-Type").orElseThrow())
        .startsWith("application/json");
    long espera = rechazado.headers().firstValueAsLong("Retry-After").orElseThrow();
    assertThat(espera).isBetween(1L, 60L);
    assertThat(rechazado.headers().firstValue("X-Trace-Id")).isPresent();
    assertThat(RECIBIDOS.get() - antes).isEqualTo(3);
    // M24 con la ruta que pone RutasGateway.
    assertThat(rechazados("donaciones", "escritura") - rechazosAntes).isEqualTo(1);
  }

  @Test
  void lecturasYEscriturasTienenCuposSeparados() throws Exception {
    String ip = "203.0.113.11";
    for (int i = 0; i < 3; i++) {
      pedir("PUT", "/api/subcategorias/3", ip);
    }
    assertThat(pedir("PATCH", "/api/camiones/1/estado", ip).statusCode()).isEqualTo(429);

    assertThat(pedir("GET", "/api/entregas/mapa", ip).statusCode()).isEqualTo(200);
  }

  @Test
  void cadaIpTieneSuCubeta() throws Exception {
    for (int i = 0; i < 5; i++) {
      pedir("GET", "/api/donaciones/contadores", "203.0.113.12");
    }
    assertThat(pedir("GET", "/api/donaciones/contadores", "203.0.113.12").statusCode())
        .isEqualTo(429);

    assertThat(pedir("GET", "/api/donaciones/contadores", "203.0.113.13").statusCode())
        .isEqualTo(200);
  }

  @Test
  void elCupoEsDelClienteYNoDeLaRuta() throws Exception {
    String ip = "203.0.113.14";
    pedir("POST", "/api/contacto", ip);
    pedir("POST", "/api/incentivos/donaciones", ip);
    pedir("POST", "/api/logistica/resultados", ip);

    assertThat(pedir("POST", "/api/personas", ip).statusCode()).isEqualTo(429);
  }

  @Test
  void informaLasFichasQueQuedan() throws Exception {
    HttpResponse<String> respuesta = pedir("GET", "/api/incentivos/ranking", "203.0.113.15");

    assertThat(respuesta.statusCode()).isEqualTo(200);
    assertThat(respuesta.headers().firstValue(LimiteDeSolicitudes.RESTANTES)).contains("4");
  }

  @Test
  void losHealthchecksNoConsumenFichas() throws Exception {
    for (int i = 0; i < 10; i++) {
      assertThat(pedir("GET", "/actuator/health", "203.0.113.16").statusCode()).isEqualTo(200);
    }
    assertThat(pedir("GET", "/api/donaciones", "203.0.113.16").statusCode()).isEqualTo(200);
  }

  private double rechazados(String ruta, String operacion) {
    Counter contador =
        registro
            .find(LimiteDeSolicitudes.RECHAZADOS)
            .tags("ruta", ruta, "cubeta", "ip", "operacion", operacion)
            .counter();
    return contador == null ? 0 : contador.count();
  }

  private HttpResponse<String> pedir(String metodo, String ruta, String ip)
      throws IOException, InterruptedException {
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .header(LimiteDeSolicitudes.X_FORWARDED_FOR, ip)
            .method(metodo, HttpRequest.BodyPublishers.noBody())
            .build();
    return http.send(pedido, HttpResponse.BodyHandlers.ofString());
  }

  private static HttpServer servicioQueCuenta() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            RECIBIDOS.incrementAndGet();
            byte[] cuerpo = "ok".getBytes(StandardCharsets.UTF_8);
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
