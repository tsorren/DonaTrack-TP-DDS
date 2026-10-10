package grupo5.donaciones.infrastructure.logistica;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.config.LogisticaProveedoresConfig;
import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaSolicitadaV1;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Carta certificada contra un RabbitMQ real: un pedido dirigido a un proveedor con cola llega solo
 * a esa cola, y uno dirigido a un proveedor sin cola vuelve devuelto en lugar de perderse.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class ProveedorLogisticaAmqpRabbitTest {

  private static final String COLA_DONATRACK = "logistica.donatrack.entregas.solicitadas";

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
    connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    connectionFactory.setPublisherReturns(true);

    RabbitMQConfig config = new RabbitMQConfig();
    DefaultClassMapper classMapper = config.classMapper();
    // Spring lo hace con el bean real: arma el mapa clase -> alias que se escribe en __TypeId__.
    classMapper.afterPropertiesSet();
    JacksonJsonMessageConverter converter = config.messageConverter(classMapper);
    template = LogisticaProveedoresConfig.templateComandos(connectionFactory, converter);

    admin = new RabbitAdmin(connectionFactory);
    TopicExchange exchange = new TopicExchange(RabbitMQConfig.EXCHANGE_DONACIONES, true, false);
    Queue cola = new Queue(COLA_DONATRACK, true);
    admin.declareExchange(exchange);
    admin.declareQueue(cola);
    admin.declareBinding(
        BindingBuilder.bind(cola).to(exchange).with("entrega.solicitada.donatrack.v1"));
  }

  @AfterAll
  static void desconectar() {
    connectionFactory.destroy();
  }

  @BeforeEach
  void vaciarCola() {
    admin.purgeQueue(COLA_DONATRACK, false);
  }

  private static DatosEntregaLogistica datos() {
    return new DatosEntregaLogistica(
        UUID.randomUUID(),
        UUID.randomUUID(),
        new DestinoEventoDTO(
            "Av. Medrano", 951, null, null, "C1179AAQ", "CABA", "Buenos Aires", "Argentina"),
        10.5,
        0.25,
        LocalDateTime.of(2026, 10, 7, 12, 0));
  }

  @Test
  void elPedidoLlegaALaColaDelProveedorElegidoConSuSobreCompleto() {
    DatosEntregaLogistica datos = datos();
    UUID envioId = UUID.randomUUID();
    var proveedor = new ProveedorLogisticaAmqp("donatrack", template, 5000);

    assertDoesNotThrow(() -> proveedor.enviar(envioId, datos, "trace-rabbit"));

    Message recibido = template.receive(COLA_DONATRACK, 5000);
    assertNotNull(recibido);
    assertEquals(envioId.toString(), recibido.getMessageProperties().getMessageId());
    assertEquals(
        "trace-rabbit",
        recibido.getMessageProperties().getHeader(FeignTraceRequestInterceptor.TRACE_HEADER));
    assertEquals(
        RabbitMQConfig.TYPE_ID_ENTREGA_SOLICITADA,
        recibido.getMessageProperties().getHeader("__TypeId__"));
    EventoEntregaSolicitadaV1 comando =
        assertInstanceOf(
            EventoEntregaSolicitadaV1.class, template.getMessageConverter().fromMessage(recibido));
    assertEquals(datos.donacionIndependienteId(), comando.donacionIndependienteId());
  }

  @Test
  void elPedidoAUnProveedorSinColaVuelveDevueltoYNoLeLlegaAOtro() {
    var sinCola = new ProveedorLogisticaAmqp("otra", template, 5000);
    UUID envioId = UUID.randomUUID();
    DatosEntregaLogistica datos = datos();

    assertThrows(EnvioRechazadoException.class, () -> sinCola.enviar(envioId, datos, "t"));

    assertNull(template.receive(COLA_DONATRACK, 500));
  }
}
