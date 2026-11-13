package grupo5.notificaciones.infrastructure.amqp;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * Reintenta un aviso cuya persona todavía no se copió en Notificaciones (R11-B08): Donaciones
 * publica {@code persona.sincronizada} desde otro hilo y puede llegar después del {@code
 * donante.registrado} de esa persona, por la misma cola y al mismo listener.
 *
 * <p>No espera dentro del hilo del consumidor: con un solo consumidor, esperar frenaría también el
 * {@code persona.sincronizada} que viene detrás. Deja salir el error para que el contenedor
 * reencole el mensaje; así pasan primero los que ya estaban en la cola o en el prefetch, entre
 * ellos la persona. Cuenta los intentos por {@code messageId} y, al llegar a {@link #INTENTOS},
 * rechaza sin reencolar y la cola lo manda a {@code notificaciones.donaciones.dlq}. Cualquier otro
 * error sale como antes, sin contarse.
 */
public final class ReintentoPersonaFaltante {

  /** Entregas de un mismo mensaje antes de mandarlo a la DLQ. */
  public static final int INTENTOS = 10;

  private static final Logger log = LoggerFactory.getLogger(ReintentoPersonaFaltante.class);

  private final int intentos;
  // Solo los mensajes que esperan a su persona: se borran al resolverse o al ir a la DLQ
  private final Map<String, Integer> fallidosPorMensaje = new ConcurrentHashMap<>();

  public ReintentoPersonaFaltante(int intentos) {
    if (intentos < 1) {
      throw new IllegalArgumentException("Hace falta al menos un intento");
    }
    this.intentos = intentos;
  }

  /** Con el tope de {@link #INTENTOS}. */
  public static ReintentoPersonaFaltante porDefecto() {
    return new ReintentoPersonaFaltante(INTENTOS);
  }

  /**
   * Corre {@code aviso} una vez. Si falta la persona y quedan intentos, relanza el error para que
   * el contenedor reencole el mensaje.
   *
   * @throws ValidationException si falta la persona y quedan intentos (el contenedor reencola)
   * @throws AmqpRejectAndDontRequeueException si falta la persona y se agotaron los intentos, o si
   *     el mensaje no trae {@code messageId} para contarlos
   */
  public void ejecutar(Runnable aviso, UUID personaId, String messageId) {
    try {
      aviso.run();
    } catch (ValidationException e) {
      if (e.getError() != ErrorCatalog.RECURSO_NO_ENCONTRADO) {
        throw e;
      }
      throw reencolarORechazar(e, personaId, messageId);
    }
    if (messageId != null) {
      fallidosPorMensaje.remove(messageId);
    }
  }

  /** Cuántas veces falló el mensaje esperando a su persona; 0 si no espera. */
  int fallidos(String messageId) {
    return fallidosPorMensaje.getOrDefault(messageId, 0);
  }

  private RuntimeException reencolarORechazar(
      ValidationException causa, UUID personaId, String messageId) {
    if (messageId == null) {
      // Sin messageId no hay cómo poner tope: reencolarlo podría girar para siempre
      log.warn(
          "[PERSONA_FALTANTE] La persona {} no está y el mensaje no trae messageId: va a la DLQ",
          personaId);
      return new AmqpRejectAndDontRequeueException("Persona sin copiar: " + personaId, causa);
    }
    int fallidos = fallidosPorMensaje.merge(messageId, 1, Integer::sum);
    if (fallidos >= intentos) {
      fallidosPorMensaje.remove(messageId);
      // Solo identificadores: ni nombre ni credenciales del aviso
      log.warn(
          "[PERSONA_FALTANTE] La persona {} no llegó tras {} intentos: el mensaje {} va a la DLQ",
          personaId,
          fallidos,
          messageId);
      return new AmqpRejectAndDontRequeueException("Persona sin copiar: " + personaId, causa);
    }
    log.info(
        "[PERSONA_FALTANTE] La persona {} todavía no está: el mensaje {} vuelve a la cola ({} de {})",
        personaId,
        messageId,
        fallidos,
        intentos);
    return causa;
  }
}
