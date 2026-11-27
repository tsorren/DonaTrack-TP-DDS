package grupo5.gateway;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.stereotype.Component;

/**
 * Las cubetas del rate limiting en Redis (E7, decisión Q11; ADR 20261115): todas las réplicas del
 * gateway cuentan contra el mismo cupo. Bucket4j guarda cada cubeta con compare-and-swap y Redis
 * borra la clave cuando la cubeta ya se recargó entera.
 *
 * <p><b>Redis caído.</b> Nada de esto tira un pedido: cualquier falla sale como excepción y {@link
 * LimiteDeSolicitudes} deja pasar el pedido (fail-open, Q11b). Para que esperar a Redis no degrade
 * el sitio:
 *
 * <ul>
 *   <li>Cada operación (conectar o consumir) espera como máximo {@code timeout}.
 *   <li>Se conecta recién con el primer pedido, así el gateway arranca aunque Redis no esté. Si no
 *       conecta, no se reintenta hasta pasado {@code reintento}: mientras tanto los pedidos pasan
 *       sin esperar. Un solo hilo intenta; los demás pasan.
 *   <li>Ya conectado, Lettuce se reconecta solo. Mientras está desconectado rechaza los comandos al
 *       instante en vez de encolarlos.
 * </ul>
 */
@Component
public class CubetasEnRedis implements LimiteDeSolicitudes.Almacen, DisposableBean {

  /** Valores en {@code donatrack.gateway.limite-de-solicitudes.redis.*}. */
  @ConfigurationProperties("donatrack.gateway.limite-de-solicitudes.redis")
  public record Config(
      @DefaultValue("localhost") String host,
      @DefaultValue("6379") int puerto,
      @DefaultValue("100ms") Duration timeout,
      @DefaultValue("5s") Duration reintento) {}

  /** Todas las claves del gateway en Redis empiezan así. */
  static final String PREFIJO = "rate-limit:";

  /** Cuánto sobrevive la clave después de que la cubeta se recargó entera. */
  private static final Duration GRACIA = Duration.ofSeconds(10);

  private final Config config;
  private final RedisClient cliente;
  private final ReentrantLock conectando = new ReentrantLock();
  private final AtomicLong proximoIntento = new AtomicLong(System.nanoTime());
  private final AtomicReference<ProxyManager<String>> cubetas = new AtomicReference<>();

  public CubetasEnRedis(Config config) {
    if (config.timeout().isZero() || config.timeout().isNegative()) {
      throw new IllegalStateException(
          "El timeout de Redis del límite de solicitudes debe ser positivo");
    }
    this.config = config;
    RedisURI uri =
        RedisURI.builder()
            .withHost(config.host())
            .withPort(config.puerto())
            .withTimeout(config.timeout())
            .build();
    this.cliente = RedisClient.create(uri);
    cliente.setOptions(
        ClientOptions.builder()
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .socketOptions(SocketOptions.builder().connectTimeout(config.timeout()).build())
            .timeoutOptions(TimeoutOptions.enabled())
            .build());
  }

  @Override
  public Bucket cubeta(String clave, Supplier<BucketConfiguration> configuracion) {
    return proxies().builder().build(PREFIJO + clave, configuracion);
  }

  private ProxyManager<String> proxies() {
    ProxyManager<String> actual = cubetas.get();
    if (actual != null) {
      return actual;
    }
    if (System.nanoTime() - proximoIntento.get() < 0 || !conectando.tryLock()) {
      throw new IllegalStateException("Redis no disponible: se reintenta conectar más tarde");
    }
    try {
      if (cubetas.get() == null) {
        cubetas.set(conectar());
      }
      return cubetas.get();
    } catch (RuntimeException falla) {
      proximoIntento.set(System.nanoTime() + config.reintento().toNanos());
      throw falla;
    } finally {
      conectando.unlock();
    }
  }

  private ProxyManager<String> conectar() {
    return Bucket4jLettuce.casBasedBuilder(
            cliente.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
        .requestTimeout(config.timeout())
        .expirationAfterWrite(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(GRACIA))
        .build();
  }

  @Override
  public void destroy() {
    cliente.shutdown();
  }
}
