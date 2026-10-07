package grupo5.logistica.infrastructure;

import grupo5.logistica.config.RabbitMQConfig;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, retira la cola {@code logistica.donaciones.asignadas}, que logística usaba antes del
 * broker para escuchar {@code donacion.asignada.v1}. Aunque el código ya no la declara, RabbitMQ la
 * conserva (es durable) junto con su binding, y seguiría acumulando copias del hecho para siempre.
 *
 * <ol>
 *   <li>Siempre la desengancha de {@code donaciones.exchange}: deja de recibir copias.
 *   <li>La borra solo si está vacía. Si tiene mensajes (por ejemplo, asignaciones publicadas por
 *       una versión de donaciones sin broker, que no tienen entrega), los deja quietos para
 *       revisión manual en lugar de perderlos.
 * </ol>
 *
 * Se puede quitar en una entrega futura, cuando todos los ambientes hayan pasado por el cambio.
 */
@Component
public class LimpiezaColaObsoleta {

  private static final Logger log = LoggerFactory.getLogger(LimpiezaColaObsoleta.class);

  private final AmqpAdmin amqpAdmin;

  public LimpiezaColaObsoleta(AmqpAdmin amqpAdmin) {
    this.amqpAdmin = amqpAdmin;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void retirarColaObsoleta() {
    String cola = RabbitMQConfig.QUEUE_OBSOLETA_DONACIONES_ASIGNADAS;
    try {
      Properties propiedades = amqpAdmin.getQueueProperties(cola);
      if (propiedades == null) {
        log.debug("[AMQP] La cola obsoleta {} no existe; nada que limpiar", cola);
        return;
      }
      amqpAdmin.removeBinding(
          new Binding(
              cola,
              Binding.DestinationType.QUEUE,
              RabbitMQConfig.EXCHANGE_DONACIONES,
              RabbitMQConfig.ROUTING_KEY_OBSOLETA_DONACION_ASIGNADA,
              null));
      Object pendientes = propiedades.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
      if (pendientes instanceof Integer cantidad && cantidad > 0) {
        log.warn(
            "[AMQP] La cola obsoleta {} quedó desenganchada pero tiene {} mensajes sin consumir. No"
                + " se borra: revisar a mano",
            cola,
            cantidad);
        return;
      }
      amqpAdmin.deleteQueue(cola, false, true);
      log.info("[AMQP] Cola obsoleta {} eliminada", cola);
    } catch (AmqpException e) {
      log.warn("[AMQP] No se pudo eliminar la cola obsoleta {}: {}", cola, e.getMessage());
    }
  }
}
