package grupo5.donaciones.consumidorunico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.config.RabbitMQConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Contra un RabbitMQ real: con dos réplicas escuchando la misma cola, una consume todo en orden y
 * la otra queda de reserva hasta que la primera se va. Y una cola ya declarada sin el argumento no
 * se puede redeclarar con él, por eso el despliegue la borra antes.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class ConsumidorUnicoActivoRabbitTest {

  private static final long ESPERA_MS = 10_000;
  private static final int MENSAJES = 10;
  private static final long PAUSA_NS = 50_000_000L;

  @Container
  static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

  private static CachingConnectionFactory connectionFactory;
  private static RabbitAdmin admin;
  private static RabbitTemplate template;

  @BeforeAll
  static void conectar() {
    connectionFactory = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
    connectionFactory.setUsername(RABBIT.getAdminUsername());
    connectionFactory.setPassword(RABBIT.getAdminPassword());
    admin = new RabbitAdmin(connectionFactory);
    template = new RabbitTemplate(connectionFactory);
  }

  @AfterAll
  static void desconectar() {
    connectionFactory.destroy();
  }

  @Test
  void conDosReplicasSoloUnaRecibeYLaOtraTomaLaColaCuandoLaPrimeraSeVa() {
    Queue cola = new RabbitMQConfig().queueRutaAsignada();
    admin.declareQueue(cola);
    List<String> recibidosA = new CopyOnWriteArrayList<>();
    List<String> recibidosB = new CopyOnWriteArrayList<>();
    SimpleMessageListenerContainer replicaA = replica(cola.getName(), recibidosA);
    SimpleMessageListenerContainer replicaB = replica(cola.getName(), recibidosB);
    try {
      replicaA.start();
      replicaB.start();
      esperar(() -> consumidores(cola.getName()) == 2, "las dos réplicas suscriptas");

      List<String> enviados = IntStream.range(0, MENSAJES).mapToObj(i -> "evento-" + i).toList();
      enviados.forEach(m -> template.convertAndSend(cola.getName(), m));
      esperar(() -> recibidosA.size() + recibidosB.size() == MENSAJES, "todos los mensajes");

      boolean activaA = !recibidosA.isEmpty();
      List<String> activa = activaA ? recibidosA : recibidosB;
      List<String> reserva = activaA ? recibidosB : recibidosA;
      assertEquals(enviados, activa);
      assertTrue(reserva.isEmpty(), "la réplica de reserva no recibe");

      (activaA ? replicaA : replicaB).stop();
      template.convertAndSend(cola.getName(), "despues-de-la-caida");
      esperar(() -> reserva.contains("despues-de-la-caida"), "la reserva toma la cola");
    } finally {
      replicaA.stop();
      replicaB.stop();
    }
  }

  @Test
  void unaColaDeclaradaSinElArgumentoNoSePuedeRedeclararConEl() {
    admin.declareQueue(new Queue(RabbitMQConfig.QUEUE_RUTA_INICIADA, true));
    Queue nueva = new RabbitMQConfig().queueRutaIniciada();

    AmqpException error = assertThrows(AmqpException.class, () -> admin.declareQueue(nueva));

    assertTrue(causas(error).contains("PRECONDITION_FAILED"), causas(error));
  }

  private static SimpleMessageListenerContainer replica(String cola, List<String> recibidos) {
    SimpleMessageListenerContainer contenedor =
        new SimpleMessageListenerContainer(connectionFactory);
    contenedor.setQueueNames(cola);
    contenedor.setConcurrentConsumers(1);
    contenedor.setMessageListener(
        mensaje -> recibidos.add(new String(mensaje.getBody(), StandardCharsets.UTF_8)));
    return contenedor;
  }

  private static int consumidores(String cola) {
    QueueInformation info = admin.getQueueInfo(cola);
    return info == null ? 0 : info.getConsumerCount();
  }

  private static String causas(Throwable error) {
    StringBuilder texto = new StringBuilder();
    for (Throwable t = error; t != null; t = t.getCause()) {
      texto.append(t).append('\n');
    }
    return texto.toString();
  }

  private static void esperar(BooleanSupplier condicion, String que) {
    long limite = System.currentTimeMillis() + ESPERA_MS;
    while (!condicion.getAsBoolean()) {
      if (System.currentTimeMillis() > limite) {
        fail("No se cumplió a tiempo: " + que);
      }
      LockSupport.parkNanos(PAUSA_NS);
    }
  }
}
