package grupo5.donaciones.services.logistica;

import java.util.Collection;
import java.util.UUID;

/**
 * Decide si un evento de vuelta lo mandó de verdad el proveedor al que le corresponde. Un evento se
 * acepta solo si el proveedor se identifica con su token y cada donación del evento está asignada a
 * ese mismo proveedor en el registro de solicitudes del broker.
 */
public interface IVerificadorOrigenEventos {

  /**
   * @param proveedorId id que declara el emisor (puede faltar)
   * @param token token del emisor (puede faltar)
   * @param donaciones donaciones sobre las que informa el evento
   * @return {@code true} solo si el emisor se autentica y todas las donaciones son suyas
   */
  boolean esOrigenValido(String proveedorId, String token, Collection<UUID> donaciones);
}
