package grupo5.tests.utils;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Consumidor espía (RM-08): una cola propia, exclusiva y temporal, ligada a los exchanges de
 * dominio, que copia cada mensaje publicado sin quitárselo a los consumidores reales.
 *
 * <p>En {@code donaciones.exchange} se liga solo a los hechos y al comando de la instancia de
 * logística del preprod: un binding {@code #} recibiría también los comandos a proveedores sin
 * cola, y el broker (que publica con {@code mandatory=true}) dejaría de ver el rechazo.
 */
public final class EspiaAmqp implements AutoCloseable {

  /**
   * Mensaje capturado; {@code tipo} es el header {@code __TypeId__} o, si falta, la routing key.
   */
  public record Mensaje(String exchange, String routingKey, String tipo, byte[] cuerpo) {}

  private static final String INSTANCIA_LOGISTICA =
      System.getProperty("logistica.instancia.id", "donatrack");

  private static final Map<String, List<String>> BINDINGS =
      Map.of(
          "donaciones.exchange",
          List.of(
              "donacion.#",
              "donante.#",
              "persona.#",
              "entrega.solicitada." + INSTANCIA_LOGISTICA + ".v1"),
          "incentivos.exchange",
          List.of("#"),
          "logistica.exchange",
          List.of("#"));

  private final Connection connection;
  private final List<Mensaje> mensajes = new CopyOnWriteArrayList<>();

  private EspiaAmqp(Connection connection) {
    this.connection = connection;
  }

  /**
   * Se conecta al broker del preprod (puerto publicado solo en 127.0.0.1) y empieza a capturar.
   * Host, puerto y usuario: {@code -Drabbitmq.host|port|user}; contraseña: {@code RABBITMQ_PASS}.
   */
  public static EspiaAmqp conectar() throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(System.getProperty("rabbitmq.host", "localhost"));
    factory.setPort(Integer.parseInt(System.getProperty("rabbitmq.port", "5672")));
    factory.setUsername(System.getProperty("rabbitmq.user", "donatrack"));
    factory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASS", ""));
    factory.setConnectionTimeout(3000);
    EspiaAmqp espia = new EspiaAmqp(factory.newConnection("espia-contratos-it"));
    espia.escuchar();
    return espia;
  }

  private void escuchar() throws IOException {
    Channel channel = connection.createChannel();
    String cola = channel.queueDeclare().getQueue();
    for (Map.Entry<String, List<String>> binding : BINDINGS.entrySet()) {
      // Misma declaración que los servicios (topic, durable): idempotente
      channel.exchangeDeclare(binding.getKey(), BuiltinExchangeType.TOPIC, true);
      for (String clave : binding.getValue()) {
        channel.queueBind(cola, binding.getKey(), clave);
      }
    }
    channel.basicConsume(
        cola,
        true,
        (tag, delivery) -> {
          Map<String, Object> headers = delivery.getProperties().getHeaders();
          Object typeId = headers == null ? null : headers.get("__TypeId__");
          String routingKey = delivery.getEnvelope().getRoutingKey();
          mensajes.add(
              new Mensaje(
                  delivery.getEnvelope().getExchange(),
                  routingKey,
                  typeId == null ? routingKey : typeId.toString(),
                  delivery.getBody()));
        },
        tag -> {});
  }

  public List<Mensaje> mensajes() {
    return List.copyOf(mensajes);
  }

  @Override
  public void close() throws IOException {
    connection.close();
  }
}
