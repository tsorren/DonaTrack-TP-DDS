package grupo5.notificaciones.infrastructure.amqp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoV1;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.impl.NotificacionRepositoryEnMemoria;
import grupo5.notificaciones.models.repositories.impl.PersonaRepositoryEnMemoria;
import grupo5.notificaciones.services.impl.NotificacionService;
import grupo5.notificaciones.services.mappers.EventoMapper;
import grupo5.notificaciones.services.mappers.MedioDeContactoMapper;
import grupo5.notificaciones.services.mappers.PersonaMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.context.ApplicationEventPublisher;

/**
 * R11-B08: el aviso de bienvenida de un donante importado espera a que su persona se copie en
 * Notificaciones y, si no llega, termina en la DLQ en lugar de perderse. Con el listener, el
 * servicio y los repositorios en memoria reales; la espera no duerme, solo se anota.
 */
class ReintentoPersonaFaltanteTest {

  private static final Log LOG = LogFactory.getLog(ReintentoPersonaFaltanteTest.class);
  private static final boolean REENCOLAR_POR_DEFECTO = true;

  private PersonaRepositoryEnMemoria personaRepository;
  private NotificacionService notificacionService;
  private final List<Duration> esperas = new ArrayList<>();
  private final UUID personaId = UUID.randomUUID();
  private final String messageId = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    personaRepository = new PersonaRepositoryEnMemoria();
    notificacionService =
        new NotificacionService(
            new NotificacionRepositoryEnMemoria(),
            new EventoMapper(personaRepository),
            mock(ApplicationEventPublisher.class),
            null,
            null);
  }

  /** C6: la persona llega durante la primera espera; queda un solo aviso. */
  @Test
  void conLaPersonaQueLlegaAntesDelUltimoIntento_dejaUnSoloAvisoDeBienvenida() {
    DonacionEventListener listener =
        listenerConEspera(
            duracion -> {
              esperas.add(duracion);
              personaRepository.save(
                  new Persona(personaId, new ArrayList<>(), "Ana Prueba", TipoPersona.HUMANA));
            });

    listener.onDonanteRegistrado(evento(), messageId);

    List<NotificacionDTO> avisos = notificacionService.obtenerNotificaciones(personaId, null);
    assertEquals(1, avisos.size());
    assertTrue(avisos.getFirst().mensaje().startsWith("Bienvenido a DonaTrack"));
    assertEquals(List.of(Duration.ofMillis(500)), esperas);
  }

  /** C7: sin persona, 4 intentos con esperas de 0,5, 1 y 2 s, y el rechazo va a la DLQ. */
  @Test
  void conLaPersonaQueNuncaLlega_rechazaSinReencolarParaQueVayaALaDlq() {
    DonacionEventListener listener = listenerConEspera(esperas::add);
    EventoDonanteRegistradoV1 evento = evento();

    AmqpRejectAndDontRequeueException rechazo =
        assertThrows(
            AmqpRejectAndDontRequeueException.class,
            () -> listener.onDonanteRegistrado(evento, messageId));

    assertEquals(ReintentoPersonaFaltante.ESPERAS, esperas);
    assertFalse(
        ContainerUtils.shouldRequeue(
            REENCOLAR_POR_DEFECTO, comoLoEnvuelveElContenedor(rechazo), LOG));
    assertTrue(notificacionService.obtenerNotificaciones(personaId, null).isEmpty());
  }

  /**
   * Duda 5 de la ficha: con el contenedor por defecto (sin reintento), la persona faltante no era
   * un error fatal y el mensaje volvía a la cola, sin espera ni tope.
   */
  @Test
  void sinReintento_elContenedorPorDefectoReencolabaLaPersonaFaltante() {
    ListenerExecutionFailedException fallo =
        comoLoEnvuelveElContenedor(new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO));

    new ConditionalRejectingErrorHandler().handleError(fallo);

    assertTrue(ContainerUtils.shouldRequeue(REENCOLAR_POR_DEFECTO, fallo, LOG));
  }

  /** C7: otro error no se reintenta y sale igual que antes. */
  @Test
  void conOtroError_noReintentaYLoDejaSalirComoAntes() {
    ReintentoPersonaFaltante reintento =
        new ReintentoPersonaFaltante(ReintentoPersonaFaltante.ESPERAS, esperas::add);
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
                    messageId));

    assertSame(otro, salida);
    assertEquals(1, intentos[0]);
    assertTrue(esperas.isEmpty());
    assertTrue(
        ContainerUtils.shouldRequeue(REENCOLAR_POR_DEFECTO, comoLoEnvuelveElContenedor(otro), LOG));
  }

  @Test
  void siLoInterrumpenMientrasEspera_dejaElErrorOriginalYRestauraLaMarca() {
    ReintentoPersonaFaltante reintento =
        new ReintentoPersonaFaltante(
            ReintentoPersonaFaltante.ESPERAS,
            duracion -> {
              throw new InterruptedException("deteniendo");
            });
    ValidationException faltante = new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO);

    try {
      ValidationException salida =
          assertThrows(
              ValidationException.class,
              () ->
                  reintento.ejecutar(
                      () -> {
                        throw faltante;
                      },
                      personaId,
                      messageId));
      assertSame(faltante, salida);
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      // Limpia la marca para no afectar a otros tests del mismo hilo
      assertTrue(Thread.interrupted());
    }
  }

  private DonacionEventListener listenerConEspera(ReintentoPersonaFaltante.Espera espera) {
    return new DonacionEventListener(
        notificacionService,
        new PersonaMapper(new MedioDeContactoMapper()),
        new ReintentoPersonaFaltante(ReintentoPersonaFaltante.ESPERAS, espera));
  }

  private EventoDonanteRegistradoV1 evento() {
    return new EventoDonanteRegistradoV1(
        UUID.randomUUID(),
        personaId,
        "Ana Prueba",
        LocalDateTime.of(2026, 11, 12, 10, 0),
        "usuario de prueba");
  }

  private static ListenerExecutionFailedException comoLoEnvuelveElContenedor(Throwable causa) {
    return new ListenerExecutionFailedException(
        "falló el listener", causa, new Message(new byte[0], new MessageProperties()));
  }
}
