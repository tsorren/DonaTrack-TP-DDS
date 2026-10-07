package grupo5.logistica.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.config.RabbitMQConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Topología de logística contra un RabbitMQ real: cada instancia recibe solo los pedidos dirigidos
 * a ella, y la cola obsoleta de {@code donacion.asignada.v1} se borra solo si está vacía.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class MensajeriaLogisticaRabbitTest {

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

  @BeforeEach
  void limpiar() {
    admin.deleteQueue(RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS);
  }

  private static RabbitMQConfig declararInstancia(String instanciaId) {
    RabbitMQConfig config = new RabbitMQConfig(instanciaId);
    TopicExchange exchange = config.donacionesExchange();
    Queue cola = config.queueEntregasSolicitadas();
    admin.declareExchange(exchange);
    admin.declareQueue(cola);
    admin.declareBinding(config.bindingEntregasSolicitadas(cola, exchange));
    admin.purgeQueue(cola.getName(), false);
    return config;
  }

  @Test
  void cadaInstanciaRecibeSoloLosPedidosDirigidosAElla() {
    RabbitMQConfig donatrack = declararInstancia("donatrack");
    RabbitMQConfig externo = declararInstancia("externo");

    template.convertAndSend(
        RabbitMQConfig.EXCHANGE_DONACIONES, externo.routingKeyEntregaSolicitada(), "pedido");

    assertNotNull(template.receive(externo.nombreColaEntregasSolicitadas(), 5000));
    assertNull(template.receive(donatrack.nombreColaEntregasSolicitadas(), 500));
  }

  /** Recrea el estado real previo al broker: la cola vieja enganchada a donacion.asignada.v1. */
  private static void declararColaObsoletaEnganchada() {
    TopicExchange exchange = new RabbitMQConfig("donatrack").donacionesExchange();
    Queue obsoleta = new Queue(RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS, true);
    admin.declareExchange(exchange);
    admin.declareQueue(obsoleta);
    admin.declareBinding(
        BindingBuilder.bind(obsoleta)
            .to(exchange)
            .with(RabbitMQConfig.ROUTING_KEY_OBSOLETA_DONACION_ASIGNADA));
  }

  private static void publicarDonacionAsignada() {
    template.convertAndSend(
        RabbitMQConfig.EXCHANGE_DONACIONES,
        RabbitMQConfig.ROUTING_KEY_OBSOLETA_DONACION_ASIGNADA,
        "asignada");
  }

  @Test
  void laLimpiezaBorraLaColaObsoleta_CuandoEstaVacia() {
    declararColaObsoletaEnganchada();

    new LimpiezaColaObsoleta(admin).retirarColaObsoleta();

    assertNull(admin.getQueueProperties(RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS));
  }

  @Test
  void laLimpiezaConservaLosMensajesPendientesPeroLaColaDejaDeRecibirCopias() {
    declararColaObsoletaEnganchada();
    publicarDonacionAsignada();
    esperarMensajesEnColaObsoleta(1);

    new LimpiezaColaObsoleta(admin).retirarColaObsoleta();
    publicarDonacionAsignada();
    publicarDonacionAsignada();

    assertNotNull(admin.getQueueProperties(RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS));
    assertNotNull(template.receive(RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS, 2000));
    assertNull(template.receive(RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS, 1000));
  }

  private static void esperarMensajesEnColaObsoleta(int esperados) {
    long limite = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < limite) {
      var props = admin.getQueueProperties(RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS);
      if (props != null
          && Integer.valueOf(esperados).equals(props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT))) {
        return;
      }
      Thread.onSpinWait();
    }
    fail("La cola obsoleta no llegó a tener " + esperados + " mensajes");
  }

  @Test
  void laLimpiezaNoFalla_CuandoLaColaObsoletaNoExiste() {
    LimpiezaColaObsoleta limpieza = new LimpiezaColaObsoleta(admin);

    assertDoesNotThrow(limpieza::retirarColaObsoleta);
  }
}
