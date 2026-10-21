package grupo5.donaciones.services.logistica;

/** El proveedor rechazó el contenido del pedido (HTTP 4xx distinto de 409). */
public class ErrorContratoProveedorException extends EnvioProveedorException {

  public ErrorContratoProveedorException(String proveedorId, String motivo) {
    super(proveedorId, motivo, null);
  }

  public ErrorContratoProveedorException(String proveedorId, String motivo, Throwable causa) {
    super(proveedorId, motivo, causa);
  }
}
