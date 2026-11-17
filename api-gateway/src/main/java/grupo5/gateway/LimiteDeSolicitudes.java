package grupo5.gateway;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.responses.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Rate limiting del gateway (E7, requerimiento 3; ADR 20261115): cada pedido que va a un servicio
 * consume una ficha de su cubeta (token bucket de Bucket4j). Sin fichas, el gateway contesta {@code
 * 429} con el {@link ErrorResponse} común y {@code Retry-After}, y el pedido no llega al servicio.
 *
 * <p><b>De quién es la cubeta.</b> De la cuenta, si el pedido trae un token que la cadena de
 * seguridad validó (el {@code sub} del JWT): así cada usuario del cliente liviano tiene la suya
 * aunque todos lleguen desde la misma IP. Si no, de la IP de origen. Un token sin validar (con la
 * seguridad apagada) no cuenta: cualquiera podría inventar un {@code sub} distinto por pedido.
 *
 * <p><b>La IP de origen.</b> Es la de la conexión, salvo que venga de un proxy confiable ({@code
 * proxies-confiables}, una expresión regular): ahí es la primera de {@code X-Forwarded-For}. El
 * cliente liviano manda la IP del navegador en ese encabezado; sin esa regla, todo el tráfico
 * anónimo de la web compartiría la cubeta de la IP del cliente liviano. Vacía (por defecto), el
 * encabezado se ignora y nadie puede esquivar el límite inventándolo.
 *
 * <p><b>Dos cupos por cubeta.</b> Lecturas ({@code GET}, {@code HEAD}, {@code OPTIONS}) y
 * escrituras (el resto), cada uno con su cantidad por {@code ventana}. Se recargan de a poco
 * (greedy), sin el doble de ráfaga en el borde de la ventana que tiene una recarga de golpe.
 *
 * <p><b>Memoria.</b> Una cubeta que pasa una ventana sin uso ya se recargó entera: es igual a una
 * nueva. Por eso se borran, una vez por ventana, las que llevan más de una sin uso. Así el mapa no
 * crece con cada IP que pasó una vez.
 *
 * <p>Las cubetas viven en la memoria de esta instancia: con más de un gateway, cada uno cuenta lo
 * suyo (ver ADR, consecuencias).
 */
@Component
public class LimiteDeSolicitudes implements HandlerFilterFunction<ServerResponse, ServerResponse> {

  /** Valores en {@code donatrack.gateway.limite-de-solicitudes.*}. */
  @ConfigurationProperties("donatrack.gateway.limite-de-solicitudes")
  public record Config(
      @DefaultValue("true") boolean habilitado,
      @DefaultValue("1m") Duration ventana,
      @DefaultValue("300") long lecturasPorUsuario,
      @DefaultValue("60") long escriturasPorUsuario,
      @DefaultValue("120") long lecturasPorIp,
      @DefaultValue("30") long escriturasPorIp,
      @DefaultValue("") String proxiesConfiables) {}

  static final String RESTANTES = "X-RateLimit-Remaining";
  static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private static final Logger log = LoggerFactory.getLogger(LimiteDeSolicitudes.class);
  private static final Set<String> LECTURAS = Set.of("GET", "HEAD", "OPTIONS");

  /** Una IPv6 completa entra holgada; lo que venga más largo en el encabezado se recorta. */
  private static final int LARGO_MAXIMO_IP = 64;

  private final Config config;
  private final Pattern proxiesConfiables;
  private final Map<String, Cubeta> cubetas = new ConcurrentHashMap<>();
  private final AtomicLong proximaLimpieza;

  public LimiteDeSolicitudes(Config config) {
    if (config.ventana().isZero() || config.ventana().isNegative()) {
      throw new IllegalStateException("La ventana del límite de solicitudes debe ser positiva");
    }
    this.config = config;
    String proxies = config.proxiesConfiables();
    this.proxiesConfiables =
        proxies == null || proxies.isBlank() ? null : Pattern.compile(proxies.trim());
    this.proximaLimpieza = new AtomicLong(System.nanoTime() + config.ventana().toNanos());
  }

  @Override
  public ServerResponse filter(ServerRequest pedido, HandlerFunction<ServerResponse> siguiente)
      throws Exception {
    if (!config.habilitado()) {
      return siguiente.handle(pedido);
    }
    String sujeto = sujetoAutenticado();
    boolean lectura = LECTURAS.contains(pedido.method().name());
    String clave =
        (sujeto != null ? "usuario:" + sujeto : "ip:" + ipDeOrigen(pedido.servletRequest()))
            + (lectura ? "|lectura" : "|escritura");
    long cupo = cupo(sujeto != null, lectura);

    ConsumptionProbe intento = cubeta(clave, cupo).tryConsumeAndReturnRemaining(1);
    if (intento.isConsumed()) {
      ServerResponse respuesta = siguiente.handle(pedido);
      informarRestantes(respuesta, intento.getRemainingTokens());
      return respuesta;
    }
    long segundos = segundosHastaLaProximaFicha(intento.getNanosToWaitForRefill());
    // Sin la IP ni el sub: alcanza con saber de qué tipo es la cubeta (datos personales, ADR
    // 20260519). El traceId del MDC une esta línea con el pedido.
    log.warn(
        "[GATEWAY] [RATE-LIMIT] [VERB: {}] [PATH: {}] [CUBETA: {}] [CUPO: {}/{}] [ESPERA: {}s]",
        pedido.method(),
        pedido.path(),
        sujeto != null ? "usuario" : "ip",
        cupo,
        config.ventana(),
        segundos);
    return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(segundos))
        .header(RESTANTES, "0")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            new ErrorResponse(
                ErrorCatalog.DEMASIADAS_SOLICITUDES.getCode(),
                "DemasiadasSolicitudes",
                "Superaste el límite de solicitudes. Probá de nuevo en " + segundos + " s.",
                LocalDateTime.now(ZoneOffset.UTC)));
  }

  /**
   * Las fichas que le quedan a la cubeta. La respuesta del servicio (la del gateway) deja sumar
   * encabezados; una que no lo permita sale sin este, que es solo informativo.
   */
  private static void informarRestantes(ServerResponse respuesta, long restantes) {
    try {
      respuesta.headers().set(RESTANTES, String.valueOf(restantes));
    } catch (UnsupportedOperationException soloLectura) {
      // Encabezados de solo lectura: el pedido sigue igual, sin el dato informativo.
    }
  }

  private long cupo(boolean porUsuario, boolean lectura) {
    if (porUsuario) {
      return lectura ? config.lecturasPorUsuario() : config.escriturasPorUsuario();
    }
    return lectura ? config.lecturasPorIp() : config.escriturasPorIp();
  }

  private Bucket cubeta(String clave, long cupo) {
    long ahora = System.nanoTime();
    limpiarSiCorresponde(ahora);
    Cubeta cubeta = cubetas.computeIfAbsent(clave, k -> new Cubeta(nueva(cupo), new AtomicLong()));
    cubeta.ultimoUso().set(ahora);
    return cubeta.bucket();
  }

  private Bucket nueva(long cupo) {
    return Bucket.builder()
        .addLimit(limite -> limite.capacity(cupo).refillGreedy(cupo, config.ventana()))
        .build();
  }

  /** Una vez por ventana, y uno solo de los hilos que llegan a la vez. */
  private void limpiarSiCorresponde(long ahora) {
    long prevista = proximaLimpieza.get();
    if (ahora - prevista >= 0
        && proximaLimpieza.compareAndSet(prevista, ahora + config.ventana().toNanos())) {
      limpiar(ahora);
    }
  }

  /** Borra las cubetas que pasaron más de una ventana sin uso: ya están llenas otra vez. */
  void limpiar(long ahora) {
    long ventana = config.ventana().toNanos();
    cubetas.values().removeIf(c -> ahora - c.ultimoUso().get() > ventana);
  }

  int cubetasActivas() {
    return cubetas.size();
  }

  /** {@code Retry-After} va en segundos enteros: se redondea para arriba y nunca es 0. */
  static long segundosHastaLaProximaFicha(long nanos) {
    return Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L);
  }

  /**
   * El {@code sub} del JWT si la cadena de seguridad validó el token del pedido; si no, null. Con
   * la seguridad apagada la autenticación es anónima (un {@code Administrador} de relleno) y
   * también da null.
   */
  static String sujetoAutenticado() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
      return null;
    }
    Jwt token = token(auth);
    String sub = token == null ? null : token.getSubject();
    return sub == null || sub.isBlank() ? null : sub;
  }

  /** El conversor de common-lib deja el Jwt como credencial; el de Spring, como principal. */
  private static Jwt token(Authentication auth) {
    if (auth.getCredentials() instanceof Jwt credencial) {
      return credencial;
    }
    return auth.getPrincipal() instanceof Jwt principal ? principal : null;
  }

  /** La IP de la conexión o, si es de un proxy confiable, la primera de {@code X-Forwarded-For}. */
  String ipDeOrigen(HttpServletRequest pedido) {
    String conexion = pedido.getRemoteAddr();
    if (proxiesConfiables == null
        || conexion == null
        || !proxiesConfiables.matcher(conexion).matches()) {
      return conexion;
    }
    String reenviada = pedido.getHeader(X_FORWARDED_FOR);
    if (reenviada == null) {
      return conexion;
    }
    String primera = reenviada.split(",", 2)[0].trim();
    if (primera.isEmpty()) {
      return conexion;
    }
    return primera.length() > LARGO_MAXIMO_IP ? primera.substring(0, LARGO_MAXIMO_IP) : primera;
  }

  private record Cubeta(Bucket bucket, AtomicLong ultimoUso) {}
}
