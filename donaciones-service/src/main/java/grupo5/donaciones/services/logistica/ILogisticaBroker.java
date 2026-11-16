package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import java.util.Optional;
import java.util.UUID;

/** Broker de integración con logística: elige a qué proveedor se le pide cada entrega. */
public interface ILogisticaBroker {

  /** Registra el pedido de entrega y lo deja pendiente de envío al primer proveedor elegido. */
  void solicitarEntrega(DatosEntregaLogistica datos);

  /** Aplica el resultado de un intento de envío informado por el relay del outbox. */
  void registrarResultado(UUID entradaId, ResultadoEnvio resultado);

  /**
   * HB-19: la necesidad de la donación se dio de baja antes de que saliera. Deja en el outbox la
   * cancelación hacia el proveedor que tiene la entrega, con el traceId actual.
   */
  ResultadoPedidoCancelacion pedirCancelacion(UUID donacionIndependienteId);

  /**
   * Proveedor al que el relay manda una cancelación, recién cuando la solicitud está {@code
   * ENVIADA}. Vacío si todavía no corresponde (la pospone) o si ya no tiene sentido (la descarta).
   */
  Optional<String> destinoDeCancelacion(UUID entradaId);

  /** El proveedor confirmó la cancelación: la solicitud se cierra. */
  void registrarCancelacionConfirmada(UUID donacionIndependienteId);
}
