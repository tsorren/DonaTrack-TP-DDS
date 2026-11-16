package grupo5.notificaciones.infrastructure.amqp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.config.RabbitMQConfig;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoV1;
import grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1;
import grupo5.notificaciones.models.repositories.impl.NotificacionRepositoryEnMemoria;
import grupo5.notificaciones.models.repositories.impl.PersonaRepositoryEnMemoria;
import grupo5.notificaciones.services.impl.NotificacionService;
import grupo5.notificaciones.services.impl.PersonasService;
import grupo5.notificaciones.services.mappers.EventoMapper;
import grupo5.notificaciones.services.mappers.MedioDeContactoMapper;
import grupo5.notificaciones.services.mappers.PersonaMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.support.ContainerUtils;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.ApplicationEventPublisher;

/**
 * R11-B08: el aviso de bienvenida de un donante importado espera a que su persona se copie en
 * Notificaciones y, si no llega, termina en la DLQ en lugar de perderse.
 *
 * <p>Los mensajes recorren una cola de un solo consumidor, como la de {@code
 * notificaciones.donaciones} con la fábrica de {@link RabbitMQConfig}: JSON con {@code __TypeId__}
 * convertido con el convertidor real, el mismo {@link DonacionEventListener} para los dos eventos y
 * la decisión de reencolar o rechazar que toma el contenedor ({@link
 * ConditionalRejectingErrorHandler} y {@link ContainerUtils#shouldRequeue}). Un mensaje reencolado
 * vuelve detrás de los que el consumidor ya tenía en su prefetch.
 */
class ReintentoPersonaFaltanteTest {

  private static final Log LOG = LogFactory.getLog(ReintentoPersonaFaltanteTest.class);
  private static final boolean REENCOLAR_POR_DEFECTO = true;
  private static final String FECHA = "2026-11-12T10:00:00Z";

  private final UUID personaId = UUID.randomUUID();
  private final String messageIdDonante = UUID.randomUUID().toString();

  private NotificacionService notificacionService;
  private PersonaRepositoryEnMemoria personaRepository;
  private ReintentoPersonaFaltante reintento;
  private ColaDeUnConsumidor cola;

  @BeforeEach
  void setUp() {
    personaRepository = new PersonaRepositoryEnMemoria();
    NotificacionRepositoryEnMemoria notificacionRepository = new NotificacionRepositoryEnMemoria();
    PersonaMapper personaMapper = new PersonaMapper(new MedioDeContactoMapper());
    notificacionService =
        new NotificacionService(
            notificacionRepository,
            new EventoMapper(personaRepository),
            mock(ApplicationEventPublisher.class),
            null,
            new PersonasService(personaRepository, notificacionRepository, personaMapper));
    reintento = ReintentoPersonaFaltante.porDefecto();
    cola =
        new ColaDeUnConsumidor(
            new DonacionEventListener(notificacionService, personaMapper, reintento));
  }

  /**
   * C6: donante.registrado llega antes que su persona por la misma cola; la persona pasa por el
   * listener mientras el aviso vuelve a la cola, y queda un solo aviso.
   */
  @Test
  void conLaPersonaQueLlegaDespuesPorLaMismaCola_dejaUnSoloAvisoDeBienvenida() {
    cola.publicar(
        RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO, donanteRegistrado(), messageIdDonante);
    cola.publicar(
        RabbitMQConfig.ROUTING_KEY_PERSONA_SINCRONIZADA,
        personaSincronizada(),
        UUID.randomUUID().toString());

    cola.consumirTodo();

    List<NotificacionDTO> avisos = notificacionService.obtenerNotificaciones(personaId, null);
    assertEquals(1, avisos.size());
    assertTrue(avisos.getFirst().mensaje().startsWith("Bienvenido a DonaTrack"));
    assertTrue(cola.dlq.isEmpty());
    // donante, persona y la segunda entrega del donante
    assertEquals(
        List.of(
            RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO,
            RabbitMQConfig.ROUTING_KEY_PERSONA_SINCRONIZADA,
            RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO),
        cola.entregadas);
    assertEquals(0, reintento.fallidos(messageIdDonante));
  }

  /**
   * C6 con varios mensajes en el medio: el aviso da vueltas por la cola sin frenar a los demás y
   * sale apenas pasa su persona.
   */
  @Test
  void conOtrosAvisosEnElMedio_laPersonaNoQuedaFrenadaDetrasDelReintento() {
    cola.publicar(
        RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO, donanteRegistrado(), messageIdDonante);
    for (int i = 0; i < 3; i++) {
      UUID otra = UUID.randomUUID();
      cola.publicar(
          RabbitMQConfig.ROUTING_KEY_PERSONA_SINCRONIZADA,
          personaSincronizada(otra),
          UUID.randomUUID().toString());
    }
    cola.publicar(
        RabbitMQConfig.ROUTING_KEY_PERSONA_SINCRONIZADA,
        personaSincronizada(),
        UUID.randomUUID().toString());

    cola.consumirTodo();

    assertEquals(1, notificacionService.obtenerNotificaciones(personaId, null).size());
    assertTrue(cola.dlq.isEmpty());
    assertEquals(6, cola.entregadas.size());
  }

  /** C7: sin persona, el mensaje se entrega INTENTOS veces y el último rechazo va a la DLQ. */
  @Test
  void conLaPersonaQueNuncaLlega_alAgotarLosIntentosVaALaDlq() {
    cola.publicar(
        RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO, donanteRegistrado(), messageIdDonante);

    cola.consumirTodo();

    assertEquals(ReintentoPersonaFaltante.INTENTOS, cola.entregadas.size());
    assertEquals(1, cola.dlq.size());
    assertEquals(messageIdDonante, cola.dlq.getFirst().getMessageProperties().getMessageId());
    assertTrue(notificacionService.obtenerNotificaciones(personaId, null).isEmpty());
    assertEquals(0, reintento.fallidos(messageIdDonante));
  }

  /** Sin messageId no hay cómo contar los intentos: va a la DLQ en lugar de girar sin tope. */
  @Test
  void sinMessageId_vaDirectoALaDlq() {
    cola.publicar(RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO, donanteRegistrado(), null);

    cola.consumirTodo();

    assertEquals(1, cola.entregadas.size());
    assertEquals(1, cola.dlq.size());
  }

  /** Mientras quedan intentos, el contenedor reencola el error de la persona faltante. */
  @Test
  void conIntentosPorDelante_dejaSalirElErrorParaQueElContenedorReencole() {
    ValidationException faltante = new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO);

    ValidationException salida =
        assertThrows(
            ValidationException.class,
            () ->
                reintento.ejecutar(
                    () -> {
                      throw faltante;
                    },
                    personaId,
                    messageIdDonante));

    assertSame(faltante, salida);
    assertEquals(1, reintento.fallidos(messageIdDonante));
    assertTrue(reencola(comoLoEnvuelveElContenedor(salida, new Message(new byte[0]))));
  }

  /**
   * Duda 5 de la ficha: con el contenedor por defecto (sin reintento), la persona faltante no era
   * un error fatal y el mensaje volvía a la cola, sin tope.
   */
  @Test
  void sinReintento_elContenedorPorDefectoReencolabaLaPersonaFaltante() {
    ListenerExecutionFailedException fallo =
        comoLoEnvuelveElContenedor(
            new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO), new Message(new byte[0]));

    new ConditionalRejectingErrorHandler().handleError(fallo);

    assertTrue(ContainerUtils.shouldRequeue(REENCOLAR_POR_DEFECTO, fallo, LOG));
  }

  /** C7: otro error no se cuenta como reintento y sale igual que antes. */
  @Test
  void conOtroError_noLoCuentaYLoDejaSalirComoAntes() {
    ValidationException otro = new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    int[] intentos = {0};

    ValidationException salida =
        assertThrows(
            ValidationException.class,
            () ->
                reintento.ejecutar(
                    () -> {
                      intentos[0]++;
                      throw otro;
                    },
                    personaId,
                    messageIdDonante));

    assertSame(otro, salida);
    assertEquals(1, intentos[0]);
    assertEquals(0, reintento.fallidos(messageIdDonante));
    assertTrue(reencola(comoLoEnvuelveElContenedor(otro, new Message(new byte[0]))));
  }

  @Test
  void conTopeMenorQueUno_noSeArma() {
    assertThrows(IllegalArgumentException.class, () -> new ReintentoPersonaFaltante(0));
  }

  private String donanteRegistrado() {
    return """
        {"donanteId":"%s","personaId":"%s","nombre":"Ana Prueba","fecha":"%s",
         "credencialesDeAcceso":"usuario de prueba"}
        """
        .formatted(UUID.randomUUID(), personaId, FECHA);
  }

  private String personaSincronizada() {
    return personaSincronizada(personaId);
  }

  private static String personaSincronizada(UUID id) {
    return """
        {"personaId":"%s","denominacion":"Ana Prueba","tipoPersona":"HUMANA",
         "mediosDeContacto":[]}
        """
        .formatted(id);
  }

  /** Lo que decide el contenedor ante el error del listener: true si reencola. */
  private static boolean reencola(ListenerExecutionFailedException fallo) {
    try {
      new ConditionalRejectingErrorHandler().handleError(fallo);
    } catch (AmqpRejectAndDontRequeueException fatal) {
      return false;
    }
    return ContainerUtils.shouldRequeue(REENCOLAR_POR_DEFECTO, fallo, LOG);
  }

  private static ListenerExecutionFailedException comoLoEnvuelveElContenedor(
      Throwable causa, Message mensaje) {
    return new ListenerExecutionFailedException("falló el listener", causa, mensaje);
  }

  /** La cola notificaciones.donaciones con su único consumidor y la DLQ. */
  private static final class ColaDeUnConsumidor {

    // Tope de seguridad: si el reintento girara sin fin, el test falla en lugar de colgarse
    private static final int MAXIMO_DE_ENTREGAS = 1_000;

    private final DonacionEventListener listener;
    private final JacksonJsonMessageConverter convertidor;
    private final Deque<Message> pendientes = new ArrayDeque<>();
    private final List<Message> dlq = new ArrayList<>();
    private final List<String> entregadas = new ArrayList<>();

    ColaDeUnConsumidor(DonacionEventListener listener) {
      this.listener = listener;
      RabbitMQConfig config = new RabbitMQConfig();
      this.convertidor = config.messageConverter(config.classMapper());
    }

    void publicar(String routingKey, String json, String messageId) {
      MessageProperties props = new MessageProperties();
      props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
      props.setHeader("__TypeId__", routingKey);
      props.setMessageId(messageId);
      pendientes.addLast(new Message(json.getBytes(StandardCharsets.UTF_8), props));
    }

    void consumirTodo() {
      while (!pendientes.isEmpty()) {
        assertTrue(entregadas.size() < MAXIMO_DE_ENTREGAS, "El mensaje giró sin tope");
        Message mensaje = pendientes.pollFirst();
        entregadas.add((String) mensaje.getMessageProperties().getHeader("__TypeId__"));
        try {
          despachar(mensaje);
        } catch (RuntimeException e) {
          if (reencola(comoLoEnvuelveElContenedor(e, mensaje))) {
            pendientes.addLast(mensaje);
          } else {
            dlq.add(mensaje);
          }
        }
      }
    }

    private void despachar(Message mensaje) {
      String messageId = mensaje.getMessageProperties().getMessageId();
      Object evento = convertidor.fromMessage(mensaje);
      if (evento instanceof EventoDonanteRegistradoV1 donante) {
        listener.onDonanteRegistrado(donante, messageId);
      } else if (evento instanceof EventoPersonaSincronizadaV1 persona) {
        listener.onPersonaSincronizada(persona, messageId);
      } else {
        throw new IllegalStateException("Evento sin handler en este test: " + evento);
      }
    }
  }
}
