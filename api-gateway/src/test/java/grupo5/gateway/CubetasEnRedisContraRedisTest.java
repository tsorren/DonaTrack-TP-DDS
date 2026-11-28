package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Contra un Redis real (Testcontainers): dos gateways que comparten Redis cuentan contra el mismo
 * cupo (Q11c), las claves vencen solas y, si Redis se cuelga con la conexión hecha, el pedido pasa
 * por timeout y el límite vuelve cuando Redis vuelve.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class CubetasEnRedisContraRedisTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @Test
  void dosGatewaysSobreElMismoRedisCompartenElCupo() throws Exception {
    CubetasEnRedis almacenUno = cubetas();
    CubetasEnRedis almacenDos = cubetas();
    try {
      LimiteDeSolicitudes uno = limite(almacenUno);
      LimiteDeSolicitudes dos = limite(almacenDos);
      String ip = "203.0.113.20";

      assertThat(filtrar(uno, ip)).isEqualTo(HttpStatus.OK);
      assertThat(filtrar(dos, ip)).isEqualTo(HttpStatus.OK);
      assertThat(filtrar(uno, ip)).isEqualTo(HttpStatus.OK);

      // Cupo de 3 escrituras entre los dos: la cuarta se rechaza en cualquiera.
      assertThat(filtrar(dos, ip)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
      assertThat(filtrar(uno, ip)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    } finally {
      almacenUno.destroy();
      almacenDos.destroy();
    }
  }

  @Test
  void laClaveVenceSola() throws Exception {
    CubetasEnRedis almacen = cubetas();
    try {
      filtrar(limite(almacen), "203.0.113.21");
    } finally {
      almacen.destroy();
    }

    RedisClient cliente =
        RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    try (StatefulRedisConnection<String, String> conexion = cliente.connect()) {
      long ttl = conexion.sync().ttl(CubetasEnRedis.PREFIJO + "ip:203.0.113.21|escritura");
      assertThat(ttl).isPositive();
    } finally {
      cliente.shutdown();
    }
  }

  @Test
  void conRedisColgadoElPedidoPasaPorTimeoutYAlVolverCuentaDeNuevo() throws Exception {
    CubetasEnRedis almacen =
        new CubetasEnRedis(
            new CubetasEnRedis.Config(
                REDIS.getHost(), REDIS.getMappedPort(6379), Duration.ofMillis(300), Duration.ZERO));
    String ip = "203.0.113.22";
    var docker = REDIS.getDockerClient();
    try {
      LimiteDeSolicitudes limite = limite(almacen);
      assertThat(filtrar(limite, ip)).isEqualTo(HttpStatus.OK);

      docker.pauseContainerCmd(REDIS.getContainerId()).exec();
      try {
        long inicio = System.nanoTime();
        for (int i = 0; i < 4; i++) {
          assertThat(filtrar(limite, ip)).isEqualTo(HttpStatus.OK);
        }
        // Cuatro pedidos con Redis colgado: cada uno corta en ~300 ms, no queda esperando.
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(4));
      } finally {
        docker.unpauseContainerCmd(REDIS.getContainerId()).exec();
      }

      // Redis volvió: la cubeta sigue con 2 fichas de 3 (las caídas no consumieron).
      assertThat(filtrar(limite, ip)).isEqualTo(HttpStatus.OK);
      assertThat(filtrar(limite, ip)).isEqualTo(HttpStatus.OK);
      assertThat(filtrar(limite, ip)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    } finally {
      almacen.destroy();
    }
  }

  private static CubetasEnRedis cubetas() {
    return new CubetasEnRedis(
        new CubetasEnRedis.Config(
            REDIS.getHost(), REDIS.getMappedPort(6379), Duration.ofSeconds(2), Duration.ZERO));
  }

  private static LimiteDeSolicitudes limite(CubetasEnRedis almacen) {
    return new LimiteDeSolicitudes(
        new LimiteDeSolicitudes.Config(true, Duration.ofMinutes(1), 3, 3, 3, 3, ""),
        almacen,
        new SimpleMeterRegistry());
  }

  private static HttpStatus filtrar(LimiteDeSolicitudes limite, String ip) throws Exception {
    MockHttpServletRequest servlet = new MockHttpServletRequest("POST", "/api/donaciones");
    servlet.setRemoteAddr(ip);
    ServerResponse respuesta =
        limite.filter(ServerRequest.create(servlet, List.of()), p -> ServerResponse.ok().build());
    return HttpStatus.valueOf(respuesta.statusCode().value());
  }
}
