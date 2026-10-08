package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;

/**
 * Aplica, de forma idempotente, los avances que informa un proveedor de logística sobre una
 * donación. Lo usan los dos caminos de vuelta: los eventos AMQP de logística y el callback HTTP.
 *
 * <p>{@code origen} identifica por dónde llegó el aviso (cola AMQP o {@code http:<proveedorId>}).
 */
public interface IProcesadorEventosLogistica {

  void procesarRutaAsignada(EventoRutaAsignada evento, String origen);

  void procesarRutaIniciada(EventoRutaIniciada evento, String origen);

  void procesarEntregaExitosa(EventoEntregaExitosa evento, String origen);

  void procesarEntregaFallida(EventoEntregaFallida evento, String origen);
}
