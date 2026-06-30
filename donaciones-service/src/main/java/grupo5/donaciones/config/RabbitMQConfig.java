package grupo5.donaciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  // Nombre del exchange (debe coincidir con el que declara logistica-service)
  public static final String EXCHANGE = "logistica.exchange";

  // Una cola por tipo de evento
  public static final String QUEUE_RUTA_INICIADA = "donaciones.ruta.iniciada";
  public static final String QUEUE_ENTREGA_EXITOSA = "donaciones.entrega.exitosa";
  public static final String QUEUE_ENTREGA_FALLIDA = "donaciones.entrega.fallida";

  // --- Exchange ---
  @Bean
  public TopicExchange logisticaExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  // --- Colas ---
  @Bean
  public Queue queueRutaIniciada() {
    return new Queue(QUEUE_RUTA_INICIADA, true);
  }

  @Bean
  public Queue queueEntregaExitosa() {
    return new Queue(QUEUE_ENTREGA_EXITOSA, true);
  }

  @Bean
  public Queue queueEntregaFallida() {
    return new Queue(QUEUE_ENTREGA_FALLIDA, true);
  }

  // --- Bindings
  @Bean
  public Binding bindingRutaIniciada(Queue queueRutaIniciada, TopicExchange logisticaExchange) {
    return BindingBuilder.bind(queueRutaIniciada).to(logisticaExchange).with("ruta.iniciada");
  }

  @Bean
  public Binding bindingEntregaExitosa(Queue queueEntregaExitosa, TopicExchange logisticaExchange) {
    return BindingBuilder.bind(queueEntregaExitosa).to(logisticaExchange).with("entrega.exitosa");
  }

  @Bean
  public Binding bindingEntregaFallida(Queue queueEntregaFallida, TopicExchange logisticaExchange) {
    return BindingBuilder.bind(queueEntregaFallida).to(logisticaExchange).with("entrega.fallida");
  }

  // --- Serialización JSON ---
  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new Jackson2JsonMessageConverter(mapper);
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    return factory;
  }
}
