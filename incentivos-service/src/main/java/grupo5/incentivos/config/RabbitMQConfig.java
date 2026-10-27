package grupo5.incentivos.config;

import grupo5.incentivos.dto.events.EventoDonacionRecibidaV1;
import grupo5.incentivos.dto.events.EventoDonacionSegmentadaV1;
import grupo5.incentivos.dto.events.EventoDonanteDadoDeBajaV1;
import grupo5.incentivos.dto.events.EventoDonanteRegistradoV1;
import grupo5.incentivos.dto.events.EventoIncentivoDonanteInactivoV1;
import grupo5.incentivos.dto.events.EventoIncentivoMisionCumplidaV1;
import grupo5.incentivos.dto.events.EventoIncentivoSubioCategoriaV1;
import grupo5.incentivos.dto.events.EventoPersonaSincronizadaV1;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(name = "incentivos.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

  // Exchanges
  public static final String EXCHANGE_INCENTIVOS = "incentivos.exchange";
  public static final String EXCHANGE_DONACIONES = "donaciones.exchange";

  // Dead Letter Exchange: recibe los mensajes que el listener rechazó sin reencolar.
  public static final String DLX_INCENTIVOS = "incentivos.dlx";

  // Cada cola suscriptora tiene su DLQ, que se llama igual más este sufijo.
  public static final String SUFIJO_DLQ = ".dlq";

  // Colas suscriptoras
  public static final String QUEUE_INCENTIVOS_DONACION_SEGMENTADA =
      "incentivos.donacion-segmentada";
  public static final String QUEUE_INCENTIVOS_DONACION_RECIBIDA = "incentivos.donacion-recibida";
  public static final String QUEUE_INCENTIVOS_PERSONA_SINCRONIZADA =
      "incentivos.persona-sincronizada";
  // Alta y baja del donante comparten cola: RabbitMQ solo garantiza el orden dentro de una cola, y
  // una baja seguida de un alta con el mismo id tiene que procesarse en el orden en que ocurrió.
  public static final String QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA =
      "incentivos.donante-ciclo-de-vida";

  public static final List<String> COLAS_SUSCRIPTORAS =
      List.of(
          QUEUE_INCENTIVOS_DONACION_SEGMENTADA,
          QUEUE_INCENTIVOS_DONACION_RECIBIDA,
          QUEUE_INCENTIVOS_PERSONA_SINCRONIZADA,
          QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA);

  // Routing Keys consumidas
  public static final String ROUTING_KEY_DONACION_SEGMENTADA = "donacion.segmentada.v1";
  public static final String ROUTING_KEY_DONACION_RECIBIDA = "donacion.recibida.v1";
  public static final String ROUTING_KEY_PERSONA_SINCRONIZADA = "persona.sincronizada.v1";
  public static final String ROUTING_KEY_DONANTE_REGISTRADO = "donante.registrado.v1";
  public static final String ROUTING_KEY_DONANTE_DADO_DE_BAJA = "donante.dado-de-baja.v1";

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

  @Bean
  public TopicExchange incentivosDlx() {
    return new TopicExchange(DLX_INCENTIVOS, true, false);
  }

  // --- Colas con Dead Letter ---
  @Bean
  public Queue queueIncentivosDonacionSegmentada() {
    return colaConDlq(QUEUE_INCENTIVOS_DONACION_SEGMENTADA);
  }

  @Bean
  public Queue queueIncentivosDonacionRecibida() {
    return colaConDlq(QUEUE_INCENTIVOS_DONACION_RECIBIDA);
  }

  @Bean
  public Queue queueIncentivosPersonaSincronizada() {
    return colaConDlq(QUEUE_INCENTIVOS_PERSONA_SINCRONIZADA);
  }

  @Bean
  public Queue queueIncentivosDonanteCicloDeVida() {
    return colaConDlq(QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA);
  }

  // --- Colas Dead Letter y sus bindings al DLX ---
  @Bean
  public Declarables deadLetterIncentivos(TopicExchange incentivosDlx) {
    List<Declarable> declarables = new ArrayList<>();
    for (String cola : COLAS_SUSCRIPTORAS) {
      Queue dlq = new Queue(nombreDlq(cola), true);
      declarables.add(dlq);
      declarables.add(BindingBuilder.bind(dlq).to(incentivosDlx).with(nombreDlq(cola)));
    }
    return new Declarables(declarables);
  }

  private static String nombreDlq(String cola) {
    return cola + SUFIJO_DLQ;
  }

  private static Queue colaConDlq(String nombre) {
    return QueueBuilder.durable(nombre)
        .withArgument("x-dead-letter-exchange", DLX_INCENTIVOS)
        .withArgument("x-dead-letter-routing-key", nombreDlq(nombre))
        .build();
  }

  // --- Bindings ---
  // La routing key de cada binding es la del evento que publica el productor (constantes de
  // arriba),
  // no el nombre de la cola. Si donaciones publica con otra key, se cambia acá y en ningún otro
  // lado.
  @Bean
  public Binding bindingIncentivosDonacionSegmentada(
      Queue queueIncentivosDonacionSegmentada, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueIncentivosDonacionSegmentada)
        .to(donacionesExchange)
        .with(ROUTING_KEY_DONACION_SEGMENTADA);
  }

  @Bean
  public Binding bindingIncentivosDonacionRecibida(
      Queue queueIncentivosDonacionRecibida, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueIncentivosDonacionRecibida)
        .to(donacionesExchange)
        .with(ROUTING_KEY_DONACION_RECIBIDA);
  }

  @Bean
  public Binding bindingIncentivosPersonaSincronizada(
      Queue queueIncentivosPersonaSincronizada, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueIncentivosPersonaSincronizada)
        .to(donacionesExchange)
        .with(ROUTING_KEY_PERSONA_SINCRONIZADA);
  }

  // Una sola cola con dos bindings: alta y baja del donante llegan en el orden en que se
  // publicaron.
  @Bean
  public Binding bindingIncentivosDonanteCicloDeVidaRegistrado(
      Queue queueIncentivosDonanteCicloDeVida, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueIncentivosDonanteCicloDeVida)
        .to(donacionesExchange)
        .with(ROUTING_KEY_DONANTE_REGISTRADO);
  }

  @Bean
  public Binding bindingIncentivosDonanteCicloDeVidaDadoDeBaja(
      Queue queueIncentivosDonanteCicloDeVida, TopicExchange donacionesExchange) {
    return BindingBuilder.bind(queueIncentivosDonanteCicloDeVida)
        .to(donacionesExchange)
        .with(ROUTING_KEY_DONANTE_DADO_DE_BAJA);
  }

  // --- Serialización y Mapeo Tipado ---
  @Bean
  public DefaultClassMapper classMapper() {
    DefaultClassMapper classMapper = new DefaultClassMapper();
    classMapper.setTrustedPackages("*");
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put(ROUTING_KEY_DONACION_SEGMENTADA, EventoDonacionSegmentadaV1.class);
    idClassMapping.put(ROUTING_KEY_DONACION_RECIBIDA, EventoDonacionRecibidaV1.class);
    idClassMapping.put(ROUTING_KEY_PERSONA_SINCRONIZADA, EventoPersonaSincronizadaV1.class);
    idClassMapping.put(ROUTING_KEY_DONANTE_REGISTRADO, EventoDonanteRegistradoV1.class);
    idClassMapping.put(ROUTING_KEY_DONANTE_DADO_DE_BAJA, EventoDonanteDadoDeBajaV1.class);
    idClassMapping.put(ROUTING_KEY_MISION_CUMPLIDA, EventoIncentivoMisionCumplidaV1.class);
    idClassMapping.put(ROUTING_KEY_SUBIO_CATEGORIA, EventoIncentivoSubioCategoriaV1.class);
    idClassMapping.put(ROUTING_KEY_DONANTE_INACTIVO, EventoIncentivoDonanteInactivoV1.class);
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
    factory.setDefaultRequeueRejected(false);
    return factory;
  }
}
