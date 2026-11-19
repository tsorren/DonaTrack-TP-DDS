package grupo5.gateway.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Servicio falso para los cuatro servicios del gateway. Responde 200 con los siete encabezados de
 * seguridad y valores distintos a los del gateway, como haría un servicio real con su propia cadena
 * de Spring Security: si el gateway los copiara, saldrían dos veces o con el valor del servicio.
 */
final class ServicioQueMandaEncabezados {

  static final Map<String, String> ESPERADOS =
      Map.of(
          "X-Content-Type-Options", "nosniff",
          "X-Frame-Options", "DENY",
          "Referrer-Policy", "no-referrer",
          "Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'",
          "Permissions-Policy", "camera=(), microphone=(), geolocation=()",
          "Cache-Control", "no-store",
          "Strict-Transport-Security", "max-age=31536000");

  private ServicioQueMandaEncabezados() {}

  static HttpServer iniciar() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            ESPERADOS
                .keySet()
                .forEach(nombre -> exchange.getResponseHeaders().add(nombre, "del-servicio"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static void apuntarLosServicios(DynamicPropertyRegistry registry, HttpServer server) {
    for (String nombre : List.of("donaciones", "notificaciones", "incentivos", "logistica")) {
      registry.add(
          "donatrack.gateway.servicios." + nombre,
          () -> "http://localhost:" + server.getAddress().getPort());
    }
  }

  /** Cada encabezado sale una sola vez y con el valor del gateway. */
  static void tieneLosSieteUnaSolaVez(HttpResponse<?> respuesta) {
    ESPERADOS.forEach((nombre, valor) -> tieneUnaSolaVez(respuesta, nombre, valor));
  }

  /**
   * Fuera de {@code /api/**} el gateway no fija el {@code Cache-Control}: quedan los otros seis.
   */
  static void tieneLosSeisSinCacheControl(HttpResponse<?> respuesta) {
    ESPERADOS.forEach(
        (nombre, valor) -> {
          if (!"Cache-Control".equals(nombre)) {
            tieneUnaSolaVez(respuesta, nombre, valor);
          }
        });
    assertThat(respuesta.headers().allValues("Cache-Control")).doesNotContain("no-store");
  }

  private static void tieneUnaSolaVez(HttpResponse<?> respuesta, String nombre, String valor) {
    assertThat(respuesta.headers().allValues(nombre))
        .as("%s en %s", nombre, respuesta.uri())
        .containsExactly(valor);
  }
}
