package grupo5.auth.correo;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.logging.Logger;

/**
 * Publica en RabbitMQ con una conexión por mensaje: el volumen es el de los correos de cuenta, y
 * así no hay conexiones viejas que revivir. Espera la confirmación del broker y pide {@code
 * mandatory}, de modo que si ninguna cola recibe el mensaje se entera, en vez de perderlo en
 * silencio.
 */
public final class PublicadorRabbitMq implements PublicadorDeEventos {

  private static final Logger LOG = Logger.getLogger(PublicadorRabbitMq.class);
  private static final String HEADER_TIPO = "__TypeId__";
  private static final String HEADER_TRACE = "X-Trace-Id";

  private final ConfiguracionAmqp config;

  public PublicadorRabbitMq(ConfiguracionAmqp config) {
    this.config = config;
  }

  @Override
  public void publicar(EventoEnlaceDeCuenta evento, String traceId) throws PublicacionFallida {
    if (!config.completa()) {
      throw new PublicacionFallida("Falta la clave de RabbitMQ en la configuración del proveedor");
    }
    try (Connection conexion = fabrica().newConnection("keycloak-correo");
        Channel canal = conexion.createChannel()) {
      canal.exchangeDeclare(config.exchange(), BuiltinExchangeType.TOPIC, true);
      canal.confirmSelect();
      AtomicBoolean sinCola = new AtomicBoolean(false);
      canal.addReturnListener(devuelto -> sinCola.set(true));

      canal.basicPublish(
          config.exchange(),
          EventoEnlaceDeCuenta.ROUTING_KEY,
          true,
          propiedades(evento, traceId),
          evento.aJson().getBytes(StandardCharsets.UTF_8));
      canal.waitForConfirmsOrDie(config.timeoutMs());

      if (sinCola.get()) {
        throw new PublicacionFallida(
            "Ninguna cola recibió el evento: ¿notificaciones-service declaró su cola?");
      }
      LOG.infof(
          "Evento de cuenta publicado: tipo=%s cuentaId=%s traceId=%s",
          evento.tipo(), evento.cuentaId(), traceId);
    } catch (IOException | TimeoutException e) {
      throw new PublicacionFallida("No se pudo publicar en RabbitMQ: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PublicacionFallida("Se interrumpió la publicación en RabbitMQ", e);
    }
  }

  private ConnectionFactory fabrica() {
    ConnectionFactory fabrica = new ConnectionFactory();
    fabrica.setHost(config.host());
    fabrica.setPort(config.puerto());
    fabrica.setVirtualHost(config.vhost());
    fabrica.setUsername(config.usuario());
    fabrica.setPassword(config.clave());
    fabrica.setConnectionTimeout(config.timeoutMs());
    fabrica.setHandshakeTimeout(config.timeoutMs());
    fabrica.setAutomaticRecoveryEnabled(false);
    return fabrica;
  }

  /** El mensaje vence con el enlace: un enlace vencido no le sirve a nadie. */
  private static AMQP.BasicProperties propiedades(EventoEnlaceDeCuenta evento, String traceId) {
    return new AMQP.BasicProperties.Builder()
        .messageId(UUID.randomUUID().toString())
        .timestamp(Date.from(evento.fecha()))
        .contentType("application/json")
        .contentEncoding("UTF-8")
        .deliveryMode(2)
        .expiration(String.valueOf(evento.venceEnMinutos() * 60_000L))
        .headers(Map.of(HEADER_TIPO, EventoEnlaceDeCuenta.ROUTING_KEY, HEADER_TRACE, traceId))
        .build();
  }
}
