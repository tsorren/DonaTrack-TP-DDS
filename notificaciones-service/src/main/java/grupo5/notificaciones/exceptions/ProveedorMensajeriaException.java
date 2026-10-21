package grupo5.notificaciones.exceptions;

/**
 * Excepción lanzada ante un fallo temporal de comunicación o disponibilidad (ej. HTTP 503 /
 * Timeout) al interactuar con un proveedor externo de mensajería.
 */
public class ProveedorMensajeriaException extends RuntimeException {

  public ProveedorMensajeriaException(String message) {
    super(message);
  }
}
