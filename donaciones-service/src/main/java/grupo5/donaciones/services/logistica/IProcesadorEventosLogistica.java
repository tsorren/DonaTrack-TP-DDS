package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;

/**
 * Aplica, de forma idempotente, los avances que informa un proveedor de logística sobre una
 * donación. Lo invoca el listener AMQP de los eventos de vuelta, una vez verificado el proveedor.
 *
 * <p>{@code origen} identifica por dónde llegó el aviso (la cola AMQP).
 */
public interface IProcesadorEventosLogistica {

  void procesarRutaAsignada(EventoRutaAsignada evento, String origen);

  void procesarRutaIniciada(EventoRutaIniciada evento, String origen);

  void procesarEntregaExitosa(EventoEntregaExitosa evento, String origen);

  void procesarEntregaFallida(EventoEntregaFallida evento, String origen);
}
