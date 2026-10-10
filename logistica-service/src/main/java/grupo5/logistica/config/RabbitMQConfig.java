package grupo5.logistica.config;

import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.eventos.EventoEntregaSolicitadaV1;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
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

  /** Alias fijo de __TypeId__ del comando, independiente de la routing key de cada instancia. */
  public static final String TYPE_ID_ENTREGA_SOLICITADA = "entrega.solicitada.v1";

  /**
   * Identidad con la que esta instancia firma los eventos que publica en logistica.exchange: su id
   * de proveedor y el token acordado con Donaciones ({@code logistica.token-vuelta}). Donaciones
   * descarta los eventos que no los traen.
   */
  public static final String HEADER_PROVEEDOR_ID = "X-Proveedor-Id";

  public static final String HEADER_PROVEEDOR_TOKEN = "X-Proveedor-Token";

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
    // Alias de los eventos que Logística publica: el tipo viaja como la routing key (no como el
    // nombre de la clase), que es lo que esperan los consumidores en sus propios mapeos.
    idClassMapping.put(ROUTING_KEY_RUTA_ASIGNADA, EventoRutaAsignada.class);
    idClassMapping.put(ROUTING_KEY_RUTA_INICIADA, EventoRutaIniciada.class);
    idClassMapping.put(ROUTING_KEY_ENTREGA_EXITOSA, EventoEntregaExitosa.class);
    idClassMapping.put(ROUTING_KEY_ENTREGA_FALLIDA, EventoEntregaFallida.class);
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
