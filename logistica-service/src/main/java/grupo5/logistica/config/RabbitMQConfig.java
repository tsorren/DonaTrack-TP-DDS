package grupo5.logistica.config;

import grupo5.logistica.dto.eventos.EventoEntregaSolicitadaV1;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE = "logistica.exchange";
  public static final String EXCHANGE_DONACIONES = "donaciones.exchange";

  /**
   * Cola que usaba logística para escuchar {@code donacion.asignada.v1} antes del broker. Ya no se
   * declara; {@code LimpiezaColaObsoleta} la desengancha al arrancar y la borra si quedó vacía.
   */
  public static final String QUEUE_OBSOLETA_DONACIONES_ASIGNADAS = "logistica.donaciones.asignadas";

  /** Routing key con la que la cola obsoleta estaba enganchada a {@code donaciones.exchange}. */
  public static final String ROUTING_KEY_OBSOLETA_DONACION_ASIGNADA = "donacion.asignada.v1";

  /** Alias fijo de __TypeId__ del comando, independiente de la routing key de cada instancia. */
  public static final String TYPE_ID_ENTREGA_SOLICITADA = "entrega.solicitada.v1";

  public static final String ROUTING_KEY_RUTA_ASIGNADA = "ruta.asignada";
  public static final String ROUTING_KEY_RUTA_INICIADA = "ruta.iniciada";
  public static final String ROUTING_KEY_ENTREGA_EXITOSA = "entrega.exitosa";
  public static final String ROUTING_KEY_ENTREGA_FALLIDA = "entrega.fallida";

  private final String instanciaId;

  public RabbitMQConfig(@Value("${logistica.instancia-id:donatrack}") String instanciaId) {
    this.instanciaId = instanciaId;
  }

  /** Cada instancia tiene su propia cola: si compartieran una, competirían por los pedidos. */
  public String nombreColaEntregasSolicitadas() {
    return "logistica." + instanciaId + ".entregas.solicitadas";
  }

  /** Clave exacta, nunca con comodines: solo los pedidos dirigidos a esta instancia. */
  public String routingKeyEntregaSolicitada() {
    return "entrega.solicitada." + instanciaId + ".v1";
  }

  @Bean
  public TopicExchange logisticaExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public TopicExchange donacionesExchange() {
    return new TopicExchange(EXCHANGE_DONACIONES, true, false);
  }

  @Bean
  public Queue queueEntregasSolicitadas() {
    return new Queue(nombreColaEntregasSolicitadas(), true);
  }

  @Bean
  public Binding bindingEntregasSolicitadas(
      Queue queueEntregasSolicitadas, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueEntregasSolicitadas)
        .to(donacionesExchange)
        .with(routingKeyEntregaSolicitada());
  }

  @Bean
  public DefaultClassMapper classMapper() {
    DefaultClassMapper classMapper = new DefaultClassMapper();
    classMapper.setTrustedPackages("*");
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put(TYPE_ID_ENTREGA_SOLICITADA, EventoEntregaSolicitadaV1.class);
    classMapper.setIdClassMapping(idClassMapping);
    return classMapper;
  }

  @Bean
  public JacksonJsonMessageConverter messageConverter(DefaultClassMapper classMapper) {
    JsonMapper mapper = JsonMapper.builder().build();
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(mapper);
    converter.setClassMapper(classMapper);
    return converter;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    return template;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    return factory;
  }
}
