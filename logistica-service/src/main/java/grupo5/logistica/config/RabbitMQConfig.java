package grupo5.logistica.config;

import grupo5.logistica.dto.eventos.EventoDonacionAsignadaV1;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE = "logistica.exchange";
  public static final String EXCHANGE_DONACIONES = "donaciones.exchange";

  public static final String QUEUE_LOGISTICA_DONACIONES_ASIGNADAS =
      "logistica.donaciones.asignadas";

  public static final String ROUTING_KEY_RUTA_ASIGNADA = "ruta.asignada";
  public static final String ROUTING_KEY_RUTA_INICIADA = "ruta.iniciada";
  public static final String ROUTING_KEY_ENTREGA_EXITOSA = "entrega.exitosa";
  public static final String ROUTING_KEY_ENTREGA_FALLIDA = "entrega.fallida";

  public static final String ROUTING_KEY_DONACION_ASIGNADA = "donacion.asignada.v1";

  @Bean
  public TopicExchange logisticaExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public TopicExchange donacionesExchange() {
    return new TopicExchange(EXCHANGE_DONACIONES, true, false);
  }

  @Bean
  public Queue queueLogisticaDonacionesAsignadas() {
    return new Queue(QUEUE_LOGISTICA_DONACIONES_ASIGNADAS, true);
  }

  @Bean
  public Binding bindingLogisticaDonacionesAsignadas(
      Queue queueLogisticaDonacionesAsignadas, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueLogisticaDonacionesAsignadas)
        .to(donacionesExchange)
        .with(ROUTING_KEY_DONACION_ASIGNADA);
  }

  @Bean
  public DefaultClassMapper classMapper() {
    DefaultClassMapper classMapper = new DefaultClassMapper();
    classMapper.setTrustedPackages("*");
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put(ROUTING_KEY_DONACION_ASIGNADA, EventoDonacionAsignadaV1.class);
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
