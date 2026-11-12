package grupo5.notificaciones.infrastructure.amqp;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * Reintenta un aviso cuya persona todavía no se copió en Notificaciones (R11-B08): Donaciones
 * publica {@code persona.sincronizada} desde otro hilo y puede llegar después del {@code
 * donante.registrado} de esa persona. Reintenta con esperas cortas y crecientes; si la persona no
 * llega, rechaza el mensaje sin reencolarlo y la cola lo manda a {@code
 * notificaciones.donaciones.dlq}. Cualquier otro error sale como antes, sin reintento.
 */
public final class ReintentoPersonaFaltante {

  /** 4 intentos: el primero y 3 reintentos, a los 0,5, 1 y 2 s. */
  public static final List<Duration> ESPERAS =
      List.of(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofSeconds(2));

  private static final Logger log = LoggerFactory.getLogger(ReintentoPersonaFaltante.class);

  /** Cómo se espera entre intentos; en los tests, sin dormir. */
  @FunctionalInterface
  public interface Espera {
    void esperar(Duration duracion) throws InterruptedException;
  }

  private final List<Duration> esperas;
  private final Espera espera;

  public ReintentoPersonaFaltante(List<Duration> esperas, Espera espera) {
    this.esperas = List.copyOf(esperas);
    this.espera = espera;
  }

  /** Con las esperas de {@link #ESPERAS}, durmiendo el hilo del consumidor. */
  public static ReintentoPersonaFaltante porDefecto() {
    return new ReintentoPersonaFaltante(ESPERAS, Thread::sleep);
  }

  /**
   * Corre {@code aviso} hasta que la persona esté o se acaben los intentos.
   *
   * @throws AmqpRejectAndDontRequeueException si la persona sigue sin llegar tras el último intento
   */
  public void ejecutar(Runnable aviso, UUID personaId, String messageId) {
    for (int intento = 0; ; intento++) {
      try {
        aviso.run();
        return;
      } catch (ValidationException e) {
        if (e.getError() != ErrorCatalog.RECURSO_NO_ENCONTRADO) {
          throw e;
        }
        if (intento >= esperas.size()) {
          // Solo identificadores: ni nombre ni credenciales del aviso
          log.warn(
              "[PERSONA_FALTANTE] La persona {} no llegó tras {} intentos: el mensaje {} va a la DLQ",
              personaId,
              intento + 1,
              messageId);
          throw new AmqpRejectAndDontRequeueException("Persona sin copiar: " + personaId, e);
        }
        esperar(esperas.get(intento), e);
      }
    }
  }

  private void esperar(Duration duracion, ValidationException causa) {
    try {
      espera.esperar(duracion);
    } catch (InterruptedException interrumpido) {
      // El consumidor se está deteniendo: sale el error original y el mensaje vuelve a la cola
      Thread.currentThread().interrupt();
      throw causa;
    }
  }
}
