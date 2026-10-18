package grupo5.donaciones.services.logistica;

/** No se sabe si el pedido llegó al proveedor (sin acuse a tiempo, timeout de lectura, 5xx). */
public class EnvioInciertoException extends EnvioProveedorException {

  public EnvioInciertoException(String proveedorId, String motivo) {
    super(proveedorId, motivo, null);
  }

  public EnvioInciertoException(String proveedorId, String motivo, Throwable causa) {
    super(proveedorId, motivo, causa);
  }
}
