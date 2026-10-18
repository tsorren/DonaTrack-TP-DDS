package grupo5.notificaciones.config;

import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.dto.input.EventoDonacionEnCaminoV1;
import grupo5.notificaciones.dto.input.EventoDonacionEntregaFallidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionVencidaV1;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoDonanteInactivoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

  // Exchanges de Emisores (Topología Pub/Sub DDD)
  public static final String EXCHANGE_DONACIONES = "donaciones.exchange";
  public static final String EXCHANGE_INCENTIVOS = "incentivos.exchange";

  // Dead Letter Exchange
  public static final String DLX_NOTIFICACIONES = "notificaciones.dlx";

  // Colas Segregadas Canónicas
  public static final String QUEUE_NOTIFICACIONES_DONACIONES = "notificaciones.donaciones";
  public static final String QUEUE_NOTIFICACIONES_INCENTIVOS = "notificaciones.incentivos";

  // Colas Dead Letter (DLQ)
  public static final String QUEUE_NOTIFICACIONES_DONACIONES_DLQ = "notificaciones.donaciones.dlq";
  public static final String QUEUE_NOTIFICACIONES_INCENTIVOS_DLQ = "notificaciones.incentivos.dlq";

  // Routing Keys Canónicas
  public static final String ROUTING_KEY_DONACION_ASIGNADA = "donacion.asignada.v1";
  public static final String ROUTING_KEY_INCENTIVOS_WILDCARD = "incentivo.#";

  // Routing Keys para Incentivos
  public static final String ROUTING_KEY_MISION_CUMPLIDA = "incentivo.mision-cumplida.v1";
  public static final String ROUTING_KEY_SUBIO_CATEGORIA = "incentivo.subio-categoria.v1";
  public static final String ROUTING_KEY_DONANTE_INACTIVO = "incentivo.donante-inactivo.v1";

  // Routing Keys de Donaciones
  public static final String ROUTING_KEY_DONACION_EN_CAMINO = "donacion.en-camino.v1";
  public static final String ROUTING_KEY_DONACION_RECIBIDA = "donacion.recibida.v1";
  public static final String ROUTING_KEY_DONACION_ENTREGA_FALLIDA = "donacion.entrega-fallida.v1";
  public static final String ROUTING_KEY_DONACION_VENCIDA = "donacion.vencida.v1";

  // Routing Keys de Donantes (viajan por la misma cola)
  public static final String ROUTING_KEY_DONANTE_REGISTRADO = "donante.registrado.v1";
  public static final String ROUTING_KEY_PERSONA_SINCRONIZADA = "persona.sincronizada.v1";

  /**
   * Claves de donaciones.exchange que tienen handler en DonacionEventListener. Se bindean una por
   * una: un comodín traería hechos ajenos (donacion.segmentada.v1, donante.dado-de-baja.v1) que no
   * tienen mapeo de tipo y terminarían en la DLQ.
   */
  public static final List<String> ROUTING_KEYS_DONACIONES_CONSUMIDAS =
      List.of(
          ROUTING_KEY_DONACION_ASIGNADA,
          ROUTING_KEY_DONACION_EN_CAMINO,
          ROUTING_KEY_DONACION_RECIBIDA,
          ROUTING_KEY_DONACION_ENTREGA_FALLIDA,
          ROUTING_KEY_DONACION_VENCIDA,
          ROUTING_KEY_DONANTE_REGISTRADO,
          ROUTING_KEY_PERSONA_SINCRONIZADA);

  // Legacy (Preservado hasta migración definitiva en Paso 3)
  public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";
  public static final String QUEUE_NOTIFICACIONES = "cola.eventos.notificaciones";
  public static final String ROUTING_KEY_NOTIFICACIONES = "notificaciones.#";

  // --- Exchanges ---
  @Bean
  public TopicExchange donacionesExchange() {
    return new TopicExchange(EXCHANGE_DONACIONES, true, false);
  }

  @Bean
  public TopicExchange incentivosExchange() {
    return new TopicExchange(EXCHANGE_INCENTIVOS, true, false);
  }

  @Bean
  public TopicExchange notificacionesDlx() {
    return new TopicExchange(DLX_NOTIFICACIONES, true, false);
  }

  @Bean
  public TopicExchange notificacionesExchange() {
    return new TopicExchange(EXCHANGE_NOTIFICACIONES, true, false);
  }

  // --- Colas Segregadas con Dead Letter ---
  @Bean
  public Queue queueNotificacionesDonaciones() {
    return QueueBuilder.durable(QUEUE_NOTIFICACIONES_DONACIONES)
        .withArgument("x-dead-letter-exchange", DLX_NOTIFICACIONES)
        .withArgument("x-dead-letter-routing-key", QUEUE_NOTIFICACIONES_DONACIONES_DLQ)
        .build();
  }

  @Bean
  public Queue queueNotificacionesIncentivos() {
    return QueueBuilder.durable(QUEUE_NOTIFICACIONES_INCENTIVOS)
        .withArgument("x-dead-letter-exchange", DLX_NOTIFICACIONES)
        .withArgument("x-dead-letter-routing-key", QUEUE_NOTIFICACIONES_INCENTIVOS_DLQ)
        .build();
  }

  // --- Colas Dead Letter ---
  @Bean
  public Queue queueNotificacionesDonacionesDlq() {
    return new Queue(QUEUE_NOTIFICACIONES_DONACIONES_DLQ, true);
  }

  @Bean
  public Queue queueNotificacionesIncentivosDlq() {
    return new Queue(QUEUE_NOTIFICACIONES_INCENTIVOS_DLQ, true);
  }

  // --- Cola Legacy ---
  @Bean
  public Queue queueNotificaciones() {
    return new Queue(QUEUE_NOTIFICACIONES, true);
  }

  // --- Bindings Principales ---
  @Bean
  public Declarables bindingsNotificacionesDonaciones(
      Queue queueNotificacionesDonaciones, TopicExchange donacionesExchange) {
    return new Declarables(
        ROUTING_KEYS_DONACIONES_CONSUMIDAS.stream()
            .map(
                key ->
                    BindingBuilder.bind(queueNotificacionesDonaciones)
                        .to(donacionesExchange)
                        .with(key))
            .toList());
  }

  @Bean
  public Binding bindingNotificacionesIncentivos(
      Queue queueNotificacionesIncentivos, TopicExchange incentivosExchange) {
    return BindingBuilder.bind(queueNotificacionesIncentivos)
        .to(incentivosExchange)
        .with(ROUTING_KEY_INCENTIVOS_WILDCARD);
  }

  // --- Bindings Dead Letter ---
  @Bean
  public Binding bindingNotificacionesDonacionesDlq(
      Queue queueNotificacionesDonacionesDlq, TopicExchange notificacionesDlx) {
    return BindingBuilder.bind(queueNotificacionesDonacionesDlq)
        .to(notificacionesDlx)
        .with(QUEUE_NOTIFICACIONES_DONACIONES_DLQ);
  }

  @Bean
  public Binding bindingNotificacionesIncentivosDlq(
      Queue queueNotificacionesIncentivosDlq, TopicExchange notificacionesDlx) {
    return BindingBuilder.bind(queueNotificacionesIncentivosDlq)
        .to(notificacionesDlx)
        .with(QUEUE_NOTIFICACIONES_INCENTIVOS_DLQ);
  }

  // --- Binding Legacy ---
  @Bean
  public Binding bindingNotificaciones(
      Queue queueNotificaciones, TopicExchange notificacionesExchange) {
    return BindingBuilder.bind(queueNotificaciones)
        .to(notificacionesExchange)
        .with(ROUTING_KEY_NOTIFICACIONES);
  }

  // --- Serialización y Mapeo Tipado ---
  @Bean
  public DefaultClassMapper classMapper() {
    DefaultClassMapper classMapper = new DefaultClassMapper();
    classMapper.setTrustedPackages("*");
    Map<String, Class<?>> idClassMapping = new HashMap<>();

    // Mapeos de Donaciones
    idClassMapping.put(ROUTING_KEY_DONACION_ASIGNADA, EventoDonacionAsignadaV1.class);
    idClassMapping.put(ROUTING_KEY_DONACION_EN_CAMINO, EventoDonacionEnCaminoV1.class);
    idClassMapping.put(ROUTING_KEY_DONACION_RECIBIDA, EventoDonacionRecibidaV1.class);
    idClassMapping.put(ROUTING_KEY_DONACION_ENTREGA_FALLIDA, EventoDonacionEntregaFallidaV1.class);
    idClassMapping.put(ROUTING_KEY_DONACION_VENCIDA, EventoDonacionVencidaV1.class);

    // Mapeos de Donantes
    idClassMapping.put(ROUTING_KEY_DONANTE_REGISTRADO, EventoDonanteRegistradoV1.class);
    idClassMapping.put(ROUTING_KEY_PERSONA_SINCRONIZADA, EventoPersonaSincronizadaV1.class);

    // Mapeos de Incentivos
    idClassMapping.put(ROUTING_KEY_DONANTE_INACTIVO, EventoIncentivoDonanteInactivoV1.class);
    idClassMapping.put(ROUTING_KEY_MISION_CUMPLIDA, EventoIncentivoMisionCumplidaV1.class);
    idClassMapping.put(ROUTING_KEY_SUBIO_CATEGORIA, EventoIncentivoSubioCategoriaV1.class);

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
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    return factory;
  }
}
