package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/** De quién es cada cubeta, qué IP cuenta y qué pasa si el almacén de cubetas falla. */
class LimiteDeSolicitudesTest {

  private static final String PROXY = "10.0.0.5";

  @AfterEach
  void limpiarSeguridad() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void sinProxiesConfiablesElXForwardedForNoCuenta() {
    LimiteDeSolicitudes limite = limite(2, "");

    assertThat(limite.ipDeOrigen(pedido(PROXY, "203.0.113.7"))).isEqualTo(PROXY);
  }

  @Test
  void desdeUnProxyConfiableCuentaLaPrimeraIpDelEncabezado() {
    LimiteDeSolicitudes limite = limite(2, "10[.].*");

    assertThat(limite.ipDeOrigen(pedido(PROXY, " 203.0.113.7 ,10.0.0.5"))).isEqualTo("203.0.113.7");
    assertThat(limite.ipDeOrigen(pedido(PROXY, null))).isEqualTo(PROXY);
    assertThat(limite.ipDeOrigen(pedido(PROXY, " ,10.0.0.9"))).isEqualTo(PROXY);
  }

  @Test
  void quienNoEsProxyConfiableNoPuedeInventarSuIp() {
    LimiteDeSolicitudes limite = limite(2, "10[.].*");

    assertThat(limite.ipDeOrigen(pedido("198.51.100.4", "203.0.113.7"))).isEqualTo("198.51.100.4");
  }

  @Test
  void unaIpDemasiadoLargaSeRecorta() {
    LimiteDeSolicitudes limite = limite(2, "10[.].*");

    assertThat(limite.ipDeOrigen(pedido(PROXY, "x".repeat(500)))).hasSize(64);
  }

  @Test
  void sinAutenticacionOAnonimaNoHayCuenta() {
    assertThat(LimiteDeSolicitudes.sujetoAutenticado()).isNull();

    autenticar(
        new AnonymousAuthenticationToken(
            "clave", "anonimo", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(LimiteDeSolicitudes.sujetoAutenticado()).isNull();
  }

  @Test
  void conTokenValidadoLaCuentaEsElSubDelJwt() {
    autenticar(new JwtAuthenticationToken(jwt("persona-1"), List.of()));
    assertThat(LimiteDeSolicitudes.sujetoAutenticado()).isEqualTo("persona-1");

    // Como lo deja Actores::autenticacion de common-lib: el Jwt va como credencial.
    autenticar(new PreAuthenticatedAuthenticationToken("actor", jwt("persona-2"), List.of()));
    assertThat(LimiteDeSolicitudes.sujetoAutenticado()).isEqualTo("persona-2");
  }

  @Test
  void unJwtSinSubNoIdentificaUnaCuenta() {
    autenticar(new JwtAuthenticationToken(jwt(null), List.of()));

    assertThat(LimiteDeSolicitudes.sujetoAutenticado()).isNull();
  }

  @Test
  void dosCuentasDesdeLaMismaIpTienenCubetasDistintas() throws Exception {
    LimiteDeSolicitudes limite = limite(1, "");

    autenticar(new JwtAuthenticationToken(jwt("persona-1"), List.of()));
    assertThat(filtrar(limite, "POST", PROXY)).isEqualTo(HttpStatus.OK);
    assertThat(filtrar(limite, "POST", PROXY)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

    autenticar(new JwtAuthenticationToken(jwt("persona-2"), List.of()));
    assertThat(filtrar(limite, "POST", PROXY)).isEqualTo(HttpStatus.OK);
  }

  @Test
  void deshabilitadoDejaPasarTodoSinTocarElAlmacen() throws Exception {
    CubetasEnMemoria almacen = new CubetasEnMemoria();
    LimiteDeSolicitudes limite =
        new LimiteDeSolicitudes(
            new LimiteDeSolicitudes.Config(false, Duration.ofMinutes(1), 1, 1, 1, 1, ""),
            almacen,
            new SimpleMeterRegistry());

    for (int i = 0; i < 5; i++) {
      assertThat(filtrar(limite, "POST", PROXY)).isEqualTo(HttpStatus.OK);
    }
    assertThat(almacen.cantidad()).isZero();
  }

  @Test
  void lecturasYEscriturasVanACubetasDistintas() throws Exception {
    CubetasEnMemoria almacen = new CubetasEnMemoria();
    LimiteDeSolicitudes limite =
        new LimiteDeSolicitudes(
            new LimiteDeSolicitudes.Config(true, Duration.ofMinutes(1), 2, 2, 2, 2, ""),
            almacen,
            new SimpleMeterRegistry());

    filtrar(limite, "GET", PROXY);
    filtrar(limite, "POST", PROXY);

    assertThat(almacen.cantidad()).isEqualTo(2);
  }

  @Test
  void siElAlmacenFallaElPedidoPasa() throws Exception {
    AtomicInteger llegaron = new AtomicInteger();
    LimiteDeSolicitudes limite =
        new LimiteDeSolicitudes(
            new LimiteDeSolicitudes.Config(true, Duration.ofMinutes(1), 1, 1, 1, 1, ""),
            (clave, configuracion) -> {
              throw new IllegalStateException("Redis caído");
            },
            new SimpleMeterRegistry());

    for (int i = 0; i < 3; i++) {
      ServerRequest pedido = ServerRequest.create(servlet("POST", PROXY), List.of());
      ServerResponse respuesta =
          limite.filter(
              pedido,
              p -> {
                llegaron.incrementAndGet();
                return ServerResponse.ok().build();
              });
      assertThat(respuesta.statusCode().value()).isEqualTo(200);
    }
    assertThat(llegaron).hasValue(3);
  }

  @Test
  void elRetryAfterRedondeaParaArribaYNuncaEsCero() {
    assertThat(LimiteDeSolicitudes.segundosHastaLaProximaFicha(0)).isEqualTo(1);
    assertThat(LimiteDeSolicitudes.segundosHastaLaProximaFicha(1)).isEqualTo(1);
    assertThat(LimiteDeSolicitudes.segundosHastaLaProximaFicha(1_000_000_000L)).isEqualTo(1);
    assertThat(LimiteDeSolicitudes.segundosHastaLaProximaFicha(1_000_000_001L)).isEqualTo(2);
  }

  @Test
  void unaVentanaNulaCortaElArranque() {
    LimiteDeSolicitudes.Config config =
        new LimiteDeSolicitudes.Config(true, Duration.ZERO, 1, 1, 1, 1, "");

    CubetasEnMemoria almacen = new CubetasEnMemoria();
    assertThatThrownBy(() -> new LimiteDeSolicitudes(config, almacen, new SimpleMeterRegistry()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void unCupoNuloCortaElArranque() {
    LimiteDeSolicitudes.Config config =
        new LimiteDeSolicitudes.Config(true, Duration.ofMinutes(1), 1, 1, 1, 0, "");
    CubetasEnMemoria almacen = new CubetasEnMemoria();

    assertThatThrownBy(() -> new LimiteDeSolicitudes(config, almacen, new SimpleMeterRegistry()))
        .isInstanceOf(IllegalStateException.class);
  }

  private static LimiteDeSolicitudes limite(long cupo, String proxies) {
    return new LimiteDeSolicitudes(
        new LimiteDeSolicitudes.Config(
            true, Duration.ofMinutes(1), cupo, cupo, cupo, cupo, proxies),
        new CubetasEnMemoria(),
        new SimpleMeterRegistry());
  }

  private static HttpStatus filtrar(LimiteDeSolicitudes limite, String metodo, String ip)
      throws Exception {
    ServerRequest pedido = ServerRequest.create(servlet(metodo, ip), List.of());
    ServerResponse respuesta = limite.filter(pedido, p -> ServerResponse.ok().build());
    return HttpStatus.valueOf(respuesta.statusCode().value());
  }

  private static MockHttpServletRequest servlet(String metodo, String ip) {
    MockHttpServletRequest servlet = new MockHttpServletRequest(metodo, "/api/donaciones");
    servlet.setRemoteAddr(ip);
    return servlet;
  }

  private static MockHttpServletRequest pedido(String conexion, String xForwardedFor) {
    MockHttpServletRequest pedido = new MockHttpServletRequest("GET", "/api/donaciones");
    pedido.setRemoteAddr(conexion);
    if (xForwardedFor != null) {
      pedido.addHeader(LimiteDeSolicitudes.X_FORWARDED_FOR, xForwardedFor);
    }
    return pedido;
  }

  private static void autenticar(Authentication auth) {
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private static Jwt jwt(String sub) {
    Jwt.Builder token = Jwt.withTokenValue("t").header("alg", "none").claim("roles", List.of());
    return sub == null ? token.build() : token.subject(sub).build();
  }
}
