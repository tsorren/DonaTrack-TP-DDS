package grupo5.logistica.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE = "logistica.exchange";

  public static final String ROUTING_KEY_RUTA_ASIGNADA = "ruta.asignada";
  public static final String ROUTING_KEY_RUTA_INICIADA = "ruta.iniciada";
  public static final String ROUTING_KEY_ENTREGA_EXITOSA = "entrega.exitosa";
  public static final String ROUTING_KEY_ENTREGA_FALLIDA = "entrega.fallida";

  @Bean
  public TopicExchange logisticaExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public JacksonJsonMessageConverter messageConverter() {
    JsonMapper mapper = JsonMapper.builder().build();
    return new JacksonJsonMessageConverter(mapper);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    return template;
  }
}
