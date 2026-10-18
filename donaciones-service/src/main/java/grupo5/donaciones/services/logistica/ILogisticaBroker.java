package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import java.util.UUID;

/** Broker de integración con logística: elige a qué proveedor se le pide cada entrega. */
public interface ILogisticaBroker {

  /** Registra el pedido de entrega y lo deja pendiente de envío al primer proveedor elegido. */
  void solicitarEntrega(DatosEntregaLogistica datos);

  /** Aplica el resultado de un intento de envío informado por el relay del outbox. */
  void registrarResultado(UUID entradaId, ResultadoEnvio resultado);
}
