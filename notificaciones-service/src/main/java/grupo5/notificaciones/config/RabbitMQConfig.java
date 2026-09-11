package grupo5.notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";
  public static final String QUEUE_NOTIFICACIONES = "cola.eventos.notificaciones";
  public static final String ROUTING_KEY_NOTIFICACIONES = "notificaciones.#";

  @Bean
  public TopicExchange notificacionesExchange() {
    return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
  }

  @Bean
  public Queue queueNotificaciones() {
    return new Queue(QUEUE_NOTIFICACIONES, true);
  }

  @Bean
  public Binding bindingNotificaciones(
      Queue queueNotificaciones, TopicExchange notificacionesExchange) {
    return BindingBuilder.bind(queueNotificaciones)
        .to(notificacionesExchange)
        .with(ROUTING_KEY_NOTIFICACIONES);
  }

  @Bean
  public JacksonJsonMessageConverter messageConverter() {
    JsonMapper mapper = JsonMapper.builder().build();
    return new JacksonJsonMessageConverter(mapper);
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
