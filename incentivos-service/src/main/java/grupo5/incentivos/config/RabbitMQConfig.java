package grupo5.incentivos.config;

import grupo5.incentivos.dto.events.EventoDonacionAsignadaV1;
import grupo5.incentivos.dto.events.EventoPersonaSincronizadaV1;
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

  // Exchanges
  public static final String EXCHANGE_INCENTIVOS = "incentivos.exchange";
  public static final String EXCHANGE_DONACIONES = "donaciones.exchange";

  // Colas suscriptoras
  public static final String QUEUE_INCENTIVOS_DONACIONES = "incentivos.donaciones";
  public static final String QUEUE_INCENTIVOS_PERSONAS = "incentivos.personas";

  // Routing Keys consumidas
  public static final String ROUTING_KEY_DONACION_ASIGNADA = "donacion.asignada.v1";
  public static final String ROUTING_KEY_PERSONA_SINCRONIZADA = "persona.sincronizada.v1";

  // Routing Keys emitidas
  public static final String ROUTING_KEY_MISION_CUMPLIDA = "incentivo.mision-cumplida.v1";
  public static final String ROUTING_KEY_SUBIO_CATEGORIA = "incentivo.subio-categoria.v1";
  public static final String ROUTING_KEY_DONANTE_INACTIVO = "incentivo.donante-inactivo.v1";

  // --- Exchanges ---
  @Bean
  public TopicExchange incentivosExchange() {
    return new TopicExchange(EXCHANGE_INCENTIVOS, true, false);
  }

  @Bean
  public TopicExchange donacionesExchange() {
    return new TopicExchange(EXCHANGE_DONACIONES, true, false);
  }

  // --- Colas ---
  @Bean
  public Queue queueIncentivosDonaciones() {
    return new Queue(QUEUE_INCENTIVOS_DONACIONES, true);
  }

  @Bean
  public Queue queueIncentivosPersonas() {
    return new Queue(QUEUE_INCENTIVOS_PERSONAS, true);
  }

  // --- Bindings ---
  @Bean
  public Binding bindingIncentivosDonaciones(
      Queue queueIncentivosDonaciones, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueIncentivosDonaciones)
        .to(donacionesExchange)
        .with(ROUTING_KEY_DONACION_ASIGNADA);
  }

  @Bean
  public Binding bindingIncentivosPersonas(
      Queue queueIncentivosPersonas, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueIncentivosPersonas)
        .to(donacionesExchange)
        .with(ROUTING_KEY_PERSONA_SINCRONIZADA);
  }

  // --- Serialización y Mapeo Tipado ---
  @Bean
  public DefaultClassMapper classMapper() {
    DefaultClassMapper classMapper = new DefaultClassMapper();
    classMapper.setTrustedPackages("*");
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put(ROUTING_KEY_DONACION_ASIGNADA, EventoDonacionAsignadaV1.class);
    idClassMapping.put(ROUTING_KEY_PERSONA_SINCRONIZADA, EventoPersonaSincronizadaV1.class);
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
