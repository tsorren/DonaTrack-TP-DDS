package grupo5.gateway;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.responses.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
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
 * <p><b>Dónde viven las cubetas.</b> En el {@link Almacen}: en Redis ({@link CubetasEnRedis}), así
 * todas las réplicas del gateway cuentan contra el mismo cupo (decisión Q11).
 *
 * <p><b>Fail-open (Q11b).</b> Si el almacén falla (Redis caído, lento o desconectado), el pedido
 * pasa sin consumir ficha: Redis no puede tirar el sitio. Queda un {@code WARN} {@code
 * [REDIS-NO-DISPONIBLE]}, uno por ventana como máximo para no inundar el log mientras dure la
 * caída.
 *
 * <p><b>Métricas (R3).</b> Cada rechazo suma uno a {@value #RECHAZADOS} (M24) con la ruta, la
 * cubeta y la operación; cada pedido que pasa sin límite porque el almacén falló suma uno a {@value
 * #FALLAS_DE_REDIS} (M25) con el tipo de falla. La ruta la pone {@link #para(String)}; el filtro
 * usado sin ruta cuenta {@value #SIN_RUTA}.
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

  /**
   * M24: pedidos rechazados, por {@code ruta}, {@code cubeta} y {@code operacion}. El nombre es el
   * que espera la alerta de O4 ({@code donatrack_gateway_rate_limit_rechazos_total}).
   */
  static final String RECHAZADOS = "donatrack.gateway.rate_limit.rechazos";

  /**
   * M25: pedidos que pasaron sin límite porque Redis falló, por {@code tipo}. El nombre es el que
   * espera la alerta de O4 ({@code donatrack_gateway_redis_fallas_total}).
   */
  static final String FALLAS_DE_REDIS = "donatrack.gateway.redis.fallas";

  static final String SIN_RUTA = "sin_ruta";
  static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private static final Logger log = LoggerFactory.getLogger(LimiteDeSolicitudes.class);
  private static final Set<String> LECTURAS = Set.of("GET", "HEAD", "OPTIONS");

  /** Una IPv6 completa entra holgada; lo que venga más largo en el encabezado se recorta. */
  private static final int LARGO_MAXIMO_IP = 64;

  /** Dónde viven las cubetas. Cualquier falla sale como excepción y el pedido pasa (fail-open). */
  @FunctionalInterface
  public interface Almacen {
    Bucket cubeta(String clave, Supplier<BucketConfiguration> configuracion);
  }

  private final Config config;
  private final Almacen almacen;
  private final Pattern proxiesConfiables;
  private final AtomicLong proximoAviso = new AtomicLong(System.nanoTime());
  private final MeterRegistry registro;

  public LimiteDeSolicitudes(Config config, Almacen almacen, MeterRegistry registro) {
    if (config.ventana().isZero() || config.ventana().isNegative()) {
      throw new IllegalStateException("La ventana del límite de solicitudes debe ser positiva");
    }
    if (Math.min(
            Math.min(config.lecturasPorUsuario(), config.escriturasPorUsuario()),
            Math.min(config.lecturasPorIp(), config.escriturasPorIp()))
        <= 0) {
      throw new IllegalStateException("Los cupos del límite de solicitudes deben ser positivos");
    }
    this.config = config;
    this.almacen = almacen;
    this.registro = registro;
    String proxies = config.proxiesConfiables();
    this.proxiesConfiables =
        proxies == null || proxies.isBlank() ? null : Pattern.compile(proxies.trim());
  }

  /** El mismo límite, con el nombre de la ruta para las métricas (el {@code route} del gateway). */
  public HandlerFilterFunction<ServerResponse, ServerResponse> para(String ruta) {
    return (pedido, siguiente) -> filtrar(pedido, siguiente, ruta);
  }

  @Override
  public ServerResponse filter(ServerRequest pedido, HandlerFunction<ServerResponse> siguiente)
      throws Exception {
    return filtrar(pedido, siguiente, SIN_RUTA);
  }

  private ServerResponse filtrar(
      ServerRequest pedido, HandlerFunction<ServerResponse> siguiente, String ruta)
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

    ConsumptionProbe intento;
    try {
      intento = almacen.cubeta(clave, () -> configuracion(cupo)).tryConsumeAndReturnRemaining(1);
    } catch (RuntimeException falla) {
      registro.counter(FALLAS_DE_REDIS, "tipo", tipoDeFalla(falla)).increment();
      avisarQueElAlmacenFalla(falla);
      return siguiente.handle(pedido);
    }
    if (intento.isConsumed()) {
      ServerResponse respuesta = siguiente.handle(pedido);
      informarRestantes(respuesta, intento.getRemainingTokens());
      return respuesta;
    }
    registro
        .counter(
            RECHAZADOS,
            "ruta",
            ruta,
            "cubeta",
            sujeto != null ? "usuario" : "ip",
            "operacion",
            lectura ? "lectura" : "escritura")
        .increment();
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

  private BucketConfiguration configuracion(long cupo) {
    return BucketConfiguration.builder()
        .addLimit(limite -> limite.capacity(cupo).refillGreedy(cupo, config.ventana()))
        .build();
  }

  /** Una línea por ventana como máximo, y la escribe uno solo de los hilos que llegan a la vez. */
  private void avisarQueElAlmacenFalla(RuntimeException falla) {
    long ahora = System.nanoTime();
    long previsto = proximoAviso.get();
    if (ahora - previsto >= 0
        && proximoAviso.compareAndSet(previsto, ahora + config.ventana().toNanos())) {
      log.warn(
          "[GATEWAY] [RATE-LIMIT] [REDIS-NO-DISPONIBLE] Los pedidos pasan sin límite (fail-open):"
              + " {}",
          falla.toString());
    }
  }

  /**
   * {@code timeout} si en la cadena de causas hay un timeout (el de Bucket4j, el de Lettuce o uno
   * de la JDK); si no, {@code error}. Se reconoce por el nombre de la clase para no atarse a una
   * versión de cada librería.
   */
  static String tipoDeFalla(Throwable falla) {
    Throwable actual = falla;
    for (int nivel = 0; actual != null && nivel < 10; nivel++) {
      if (actual instanceof TimeoutException
          || actual.getClass().getSimpleName().contains("Timeout")) {
        return "timeout";
      }
      actual = actual.getCause() == actual ? null : actual.getCause();
    }
    return "error";
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
}
