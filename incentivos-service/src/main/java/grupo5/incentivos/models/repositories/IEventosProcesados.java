package grupo5.incentivos.models.repositories;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.UUID;

/**
 * Inbox de eventos consumidos: permite que un evento que se entrega más de una vez modifique al
 * donante una sola vez.
 */
public interface IEventosProcesados {

  /**
   * Consulta rápida para descartar un duplicado antes de tocar al donante. No alcanza para
   * deduplicar: dos entregas simultáneas pueden pasarla a la vez, y la garantía la da {@link
   * #guardarSiNoProcesado}.
   */
  boolean yaProcesado(UUID eventoId);

  /**
   * Guarda el donante y marca el evento como procesado en una única operación atómica: o quedan las
   * dos cosas, o ninguna. Si hay outbox, también registra ahí los avisos de los eventos de dominio
   * del donante, en esa misma operación.
   *
   * @param eventoId identificador del evento (el messageId AMQP del emisor); si es {@code null} no
   *     se deduplica, pero el guardado y los avisos siguen siendo atómicos
   * @param donante agregado ya modificado por el evento
   * @return {@code true} si el donante se guardó; {@code false} si el evento ya estaba procesado,
   *     en cuyo caso no se guarda nada
   */
  boolean guardarSiNoProcesado(UUID eventoId, DonanteIncentivos donante);
}
