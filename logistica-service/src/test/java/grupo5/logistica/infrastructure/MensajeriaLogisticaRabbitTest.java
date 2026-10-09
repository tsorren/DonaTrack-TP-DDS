package grupo5.logistica.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.config.RabbitMQConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
 * a ella.
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
}
