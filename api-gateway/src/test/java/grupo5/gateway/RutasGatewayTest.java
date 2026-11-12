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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Rutas del gateway contra cuatro servicios falsos (un {@link HttpServer} del JDK por servicio).
 * Cada servicio falso responde con su nombre, la ruta que le llegó y el {@code X-Trace-Id}
 * recibido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RutasGatewayTest {

  private static final String TRACE_HEADER = "X-Trace-Id";
  private static final String TRACE_RECIBIDO = "X-Trace-Recibido";
  private static final String QUERY_RECIBIDA = "X-Query-Recibida";
  private static final String TOTAL = "X-Total-Count";
  private static final Map<String, HttpServer> SERVICIOS =
      Map.of(
          "donaciones", servicioFalso("donaciones"),
          "notificaciones", servicioFalso("notificaciones"),
          "incentivos", servicioFalso("incentivos"),
          "logistica", servicioFalso("logistica"));

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int puerto;

  @Autowired private GatewayProperties props;

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

  @ParameterizedTest(name = "{0} {1} -> {2}")
  @CsvSource({
    "GET, /api/donaciones, donaciones",
    "GET, /api/donaciones/42, donaciones",
    "POST, /api/donantes/archivos, donaciones",
    "GET, /api/entidades/publicas, donaciones",
    "GET, /api/donaciones/contadores, donaciones",
    "GET, /api/donaciones/resumen, donaciones",
    "GET, /api/donaciones/destacadas, donaciones",
    "PUT, /api/subcategorias/3, donaciones",
    "GET, /api/asignaciones/propuestas, donaciones",
    "GET, /api/logistica/proveedores, donaciones",
    "PUT, /api/logistica/proveedor-preferido, donaciones",
    "POST, /api/notificaciones/eventos, notificaciones",
    "GET, /api/notificaciones/persona/7/no-leidas/conteo, notificaciones",
    "POST, /api/contacto, notificaciones",
    "GET, /api/contacto/4821, notificaciones",
    "POST, /api/contacto/4821/respuesta, notificaciones",
    "GET, /api/incentivos/ranking/ultimo, incentivos",
    "GET, /api/entregas/mapa, logistica",
    "PATCH, /api/camiones/1/estado, logistica",
    "GET, /api/rutas/5/entregas, logistica",
    "POST, /api/logistica/resultados, logistica",
    "POST, /api/logistica/callback/rutas, logistica",
    "GET, /api/logistica/planificaciones/9, logistica",
    "POST, /api/logistica/planificaciones/ejecuciones, logistica"
  })
  void enrutaSinReescrituraAlServicioQueCorresponde(String metodo, String ruta, String servicio)
      throws Exception {
    HttpResponse<String> respuesta = pedir(metodo, ruta, null);

    assertThat(respuesta.statusCode()).isEqualTo(200);
    assertThat(respuesta.body()).isEqualTo(servicio + " " + ruta);
  }

  @ParameterizedTest
  @CsvSource({
    "POST, /notificaciones",
    "GET, /notificaciones/persona/7",
    "GET, /donaciones-independientes",
    "GET, /api/logistica/otra",
    "GET, /api/inexistente"
  })
  void rutaFueraDeLaTablaDa404(String metodo, String ruta) throws Exception {
    assertThat(pedir(metodo, ruta, null).statusCode()).isEqualTo(404);
  }

  @Test
  void propagaElTraceIdDelCliente() throws Exception {
    String traceId = "0123456789abcdef0123456789abcdef";

    HttpResponse<String> respuesta = pedir("GET", "/api/donaciones", traceId);

    assertThat(respuesta.headers().firstValue(TRACE_RECIBIDO)).contains(traceId);
    assertThat(respuesta.headers().allValues(TRACE_HEADER)).containsExactly(traceId);
  }

  @Test
  void unListadoPaginadoLlegaConSusParametrosYVuelveConElTotal_R1101() throws Exception {
    String query = "page=2&size=50&q=g%C3%B3mez&orden=NOMBRE&estado=EN_DEPOSITO&estado=ENTREGADA";

    HttpResponse<String> respuesta = pedir("GET", "/api/donantes?" + query, null);

    assertThat(respuesta.statusCode()).isEqualTo(200);
    // El gateway puede reordenar los parámetros: llegan todos, con sus valores repetidos
    assertThat(respuesta.headers().firstValue(QUERY_RECIBIDA).orElseThrow().split("&"))
        .containsExactlyInAnyOrder(query.split("&"));
    assertThat(respuesta.headers().firstValue(TOTAL)).contains("57");
  }

  @Test
  void generaElTraceIdSiFaltaYLoMandaAlServicio() throws Exception {
    HttpResponse<String> respuesta = pedir("GET", "/api/incentivos/ranking/ultimo", null);

    List<String> devuelto = respuesta.headers().allValues(TRACE_HEADER);
    assertThat(devuelto).hasSize(1);
    assertThat(devuelto.getFirst()).matches("[0-9a-f]{32}");
    assertThat(respuesta.headers().firstValue(TRACE_RECIBIDO)).contains(devuelto.getFirst());
  }

  @Test
  void cadaRutaPublicaTieneRutaExpuesta() throws Exception {
    assertThat(props.rutasPublicas()).isNotEmpty();
    for (String rutaPublica : props.rutasPublicas()) {
      String[] partes = rutaPublica.split(" ");
      assertThat(pedir(partes[0], partes[1], null).statusCode()).as(rutaPublica).isEqualTo(200);
    }
  }

  private HttpResponse<String> pedir(String metodo, String ruta, String traceId)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .method(metodo, HttpRequest.BodyPublishers.noBody());
    if (traceId != null) {
      builder.header(TRACE_HEADER, traceId);
    }
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static HttpServer servicioFalso(String nombre) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            String traceRecibido = exchange.getRequestHeaders().getFirst(TRACE_HEADER);
            if (traceRecibido != null) {
              exchange.getResponseHeaders().add(TRACE_RECIBIDO, traceRecibido);
              // Como un servicio real: devuelve el traceId que adoptó.
              exchange.getResponseHeaders().add(TRACE_HEADER, traceRecibido);
            }
            String query = exchange.getRequestURI().getRawQuery();
            if (query != null) {
              exchange.getResponseHeaders().add(QUERY_RECIBIDA, query);
            }
            // Como un listado paginado (R11-01): el total va en un header
            exchange.getResponseHeaders().add(TOTAL, "57");
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
