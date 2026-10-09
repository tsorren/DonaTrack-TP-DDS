package grupo5.donaciones.services.logistica;

/**
 * Es seguro que el pedido no llegó al proveedor (mensaje devuelto, nack, conexión rechazada, 503).
 */
public class EnvioRechazadoException extends EnvioProveedorException {

  public EnvioRechazadoException(String proveedorId, String motivo) {
    super(proveedorId, motivo, null);
  }

  public EnvioRechazadoException(String proveedorId, String motivo, Throwable causa) {
    super(proveedorId, motivo, causa);
  }
}
