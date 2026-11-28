package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/** M24 (pedidos rechazados) y M25 (fallas de Redis) del rate limiting, R3. */
class LimiteDeSolicitudesMetricasTest {

  private static final String IP = "203.0.113.20";

  private final SimpleMeterRegistry registro = new SimpleMeterRegistry();

  @AfterEach
  void limpiarSeguridad() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void cadaRechazoCuentaConLaRutaLaCubetaYLaOperacion() throws Exception {
    HandlerFilterFunction<ServerResponse, ServerResponse> donaciones =
        limite(1, new CubetasEnMemoria()).para("donaciones");

    assertThat(filtrar(donaciones, "POST")).isEqualTo(200);
    assertThat(filtrar(donaciones, "POST")).isEqualTo(429);
    assertThat(filtrar(donaciones, "POST")).isEqualTo(429);

    assertThat(rechazados("donaciones", "ip", "escritura")).isEqualTo(2);
    assertThat(rechazados("donaciones", "ip", "lectura")).isZero();
  }

  @Test
  void unPedidoQuePasaNoCuentaComoRechazo() throws Exception {
    HandlerFilterFunction<ServerResponse, ServerResponse> incentivos =
        limite(5, new CubetasEnMemoria()).para("incentivos");

    for (int i = 0; i < 5; i++) {
      assertThat(filtrar(incentivos, "GET")).isEqualTo(200);
    }

    assertThat(registro.find(LimiteDeSolicitudes.RECHAZADOS).counters()).isEmpty();
  }

  @Test
  void elRechazoDeUnaCuentaSeEtiquetaComoUsuario() throws Exception {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("cuenta-1").build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
    HandlerFilterFunction<ServerResponse, ServerResponse> logistica =
        limite(1, new CubetasEnMemoria()).para("logistica");

    filtrar(logistica, "GET");
    assertThat(filtrar(logistica, "GET")).isEqualTo(429);

    assertThat(rechazados("logistica", "usuario", "lectura")).isEqualTo(1);
  }

  @Test
  void elFiltroSinRutaCuentaComoSinRuta() throws Exception {
    LimiteDeSolicitudes limite = limite(1, new CubetasEnMemoria());

    limite.filter(pedido("DELETE"), p -> ServerResponse.ok().build());
    limite.filter(pedido("DELETE"), p -> ServerResponse.ok().build());

    assertThat(rechazados(LimiteDeSolicitudes.SIN_RUTA, "ip", "escritura")).isEqualTo(1);
  }

  @Test
  void cadaPedidoQuePasaPorUnaFallaDeRedisCuentaUnaVez() throws Exception {
    HandlerFilterFunction<ServerResponse, ServerResponse> donaciones =
        limite(
                1,
                (clave, configuracion) -> {
                  throw new IllegalStateException("Redis no disponible");
                })
            .para("donaciones");

    for (int i = 0; i < 3; i++) {
      assertThat(filtrar(donaciones, "POST")).isEqualTo(200);
    }

    assertThat(fallas("error")).isEqualTo(3);
    assertThat(fallas("timeout")).isZero();
    assertThat(registro.find(LimiteDeSolicitudes.RECHAZADOS).counters()).isEmpty();
  }

  @Test
  void unTimeoutDeRedisCuentaComoTimeout() throws Exception {
    LimiteDeSolicitudes.Almacen lento =
        (clave, configuracion) -> {
          throw new RedisCommandTimeoutException("Command timed out after 100 millisecond(s)");
        };

    filtrar(limite(1, lento).para("notificaciones"), "GET");

    assertThat(fallas("timeout")).isEqualTo(1);
    assertThat(fallas("error")).isZero();
  }

  @Test
  void elTipoDeFallaMiraLaCadenaDeCausas() {
    assertThat(
            LimiteDeSolicitudes.tipoDeFalla(
                new RedisConnectionException(
                    "Unable to connect", new RedisCommandTimeoutException("init timed out"))))
        .isEqualTo("timeout");
    assertThat(
            LimiteDeSolicitudes.tipoDeFalla(
                new IllegalStateException("envuelta", new TimeoutException("de la JDK"))))
        .isEqualTo("timeout");
    assertThat(LimiteDeSolicitudes.tipoDeFalla(new RedisConnectionException("Connection refused")))
        .isEqualTo("error");
    assertThat(LimiteDeSolicitudes.tipoDeFalla(new IllegalStateException("Redis no disponible")))
        .isEqualTo("error");
  }

  @Test
  void deshabilitadoNoCuentaNada() throws Exception {
    LimiteDeSolicitudes limite =
        new LimiteDeSolicitudes(
            new LimiteDeSolicitudes.Config(false, Duration.ofMinutes(1), 1, 1, 1, 1, ""),
            (clave, configuracion) -> {
              throw new IllegalStateException("no se usa");
            },
            registro);

    filtrar(limite.para("donaciones"), "POST");
    filtrar(limite.para("donaciones"), "POST");

    assertThat(registro.getMeters()).isEmpty();
  }

  private LimiteDeSolicitudes limite(long cupo, LimiteDeSolicitudes.Almacen almacen) {
    return new LimiteDeSolicitudes(
        new LimiteDeSolicitudes.Config(true, Duration.ofMinutes(1), cupo, cupo, cupo, cupo, ""),
        almacen,
        registro);
  }

  private double rechazados(String ruta, String cubeta, String operacion) {
    Counter contador =
        registro
            .find(LimiteDeSolicitudes.RECHAZADOS)
            .tags("ruta", ruta, "cubeta", cubeta, "operacion", operacion)
            .counter();
    return contador == null ? 0 : contador.count();
  }

  private double fallas(String tipo) {
    Counter contador =
        registro.find(LimiteDeSolicitudes.FALLAS_DE_REDIS).tag("tipo", tipo).counter();
    return contador == null ? 0 : contador.count();
  }

  private static int filtrar(
      HandlerFilterFunction<ServerResponse, ServerResponse> filtro, String metodo)
      throws Exception {
    return filtro.filter(pedido(metodo), p -> ServerResponse.ok().build()).statusCode().value();
  }

  private static ServerRequest pedido(String metodo) {
    MockHttpServletRequest servlet = new MockHttpServletRequest(metodo, "/api/algo");
    servlet.setRemoteAddr(IP);
    return ServerRequest.create(servlet, List.of());
  }
}
