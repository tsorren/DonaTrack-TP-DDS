package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import java.util.UUID;

/**
 * Puerto que implementa cada adapter de proveedor de logística (AMQP, HTTP). El broker solo conoce
 * esta interfaz.
 */
public interface IProveedorLogistica {

  String id();

  /**
   * Envía el pedido de entrega al proveedor.
   *
   * @throws EnvioRechazadoException si es seguro que el pedido no llegó
   * @throws EnvioInciertoException si no se sabe si el pedido llegó
   * @throws ErrorContratoProveedorException si el proveedor rechazó el contenido del pedido
   */
  void enviar(UUID envioId, DatosEntregaLogistica datos, String traceId);
}
