package grupo5.logistica.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE = "logistica.exchange";

  public static final String ROUTING_KEY_RUTA_INICIADA = "ruta.iniciada";
  public static final String ROUTING_KEY_ENTREGA_EXITOSA = "entrega.exitosa";
  public static final String ROUTING_KEY_ENTREGA_FALLIDA = "entrega.fallida";

  /** Exchange de tipo Topic: un solo exchange, múltiples routing keys. */
  @Bean
  public TopicExchange logisticaExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new Jackson2JsonMessageConverter(mapper);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    return template;
  }
}
