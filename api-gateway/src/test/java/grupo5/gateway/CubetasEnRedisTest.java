package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bucket4j.BucketConfiguration;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Redis caído o mudo, sin Docker: cada falla sale rápido como excepción (el filtro la convierte en
 * fail-open) y, después de una falla de conexión, no se reintenta enseguida.
 */
class CubetasEnRedisTest {

  private static final Duration TIMEOUT = Duration.ofMillis(200);
  private static final Supplier<BucketConfiguration> UNA_FICHA =
      () ->
          BucketConfiguration.builder()
              .addLimit(l -> l.capacity(1).refillGreedy(1, Duration.ofMinutes(1)))
              .build();

  @Test
  void sinRedisFallaRapidoYNoReintentaEnseguida() throws IOException {
    CubetasEnRedis cubetas = cubetas(puertoCerrado(), Duration.ofMinutes(1));
    try {
      long inicio = System.nanoTime();
      assertThatThrownBy(() -> cubetas.cubeta("ip:x|lectura", UNA_FICHA).tryConsume(1))
          .isInstanceOf(RuntimeException.class);
      assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));

      // Dentro del plazo de reintento ni siquiera intenta: falla al instante.
      long segundo = System.nanoTime();
      assertThatThrownBy(() -> cubetas.cubeta("ip:x|lectura", UNA_FICHA))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Redis no disponible");
      assertThat(Duration.ofNanos(System.nanoTime() - segundo)).isLessThan(Duration.ofMillis(50));
    } finally {
      cubetas.destroy();
    }
  }

  @Test
  void unRedisQueAceptaLaConexionYNoContestaCortaPorTimeout() throws Exception {
    try (ServerSocket mudo = new ServerSocket(0)) {
      List<Socket> aceptados = new CopyOnWriteArrayList<>();
      Thread aceptar =
          Thread.ofVirtual()
              .start(
                  () -> {
                    try {
                      while (!mudo.isClosed()) {
                        aceptados.add(mudo.accept());
                      }
                    } catch (IOException cerrado) {
                      // El test terminó y cerró el socket.
                    }
                  });
      CubetasEnRedis cubetas = cubetas(mudo.getLocalPort(), Duration.ZERO);
      try {
        for (int i = 0; i < 2; i++) {
          long inicio = System.nanoTime();
          assertThatThrownBy(() -> cubetas.cubeta("ip:y|escritura", UNA_FICHA).tryConsume(1))
              .isInstanceOf(RuntimeException.class);
          assertThat(Duration.ofNanos(System.nanoTime() - inicio))
              .isLessThan(Duration.ofSeconds(2));
        }

        // A través del filtro: el pedido pasa (fail-open) aunque el cupo sea de una ficha.
        LimiteDeSolicitudes limite =
            new LimiteDeSolicitudes(
                new LimiteDeSolicitudes.Config(true, Duration.ofMinutes(1), 1, 1, 1, 1, ""),
                cubetas);
        for (int i = 0; i < 3; i++) {
          MockHttpServletRequest servlet = new MockHttpServletRequest("POST", "/api/donaciones");
          servlet.setRemoteAddr("198.51.100.9");
          ServerResponse respuesta =
              limite.filter(
                  ServerRequest.create(servlet, List.of()), p -> ServerResponse.ok().build());
          assertThat(respuesta.statusCode().value()).isEqualTo(200);
        }
      } finally {
        cubetas.destroy();
        mudo.close();
        aceptar.join(1000);
        for (Socket s : aceptados) {
          s.close();
        }
      }
    }
  }

  private static CubetasEnRedis cubetas(int puerto, Duration reintento) {
    return new CubetasEnRedis(new CubetasEnRedis.Config("localhost", puerto, TIMEOUT, reintento));
  }

  private static int puertoCerrado() throws IOException {
    try (ServerSocket libre = new ServerSocket(0)) {
      return libre.getLocalPort();
    }
  }
}
