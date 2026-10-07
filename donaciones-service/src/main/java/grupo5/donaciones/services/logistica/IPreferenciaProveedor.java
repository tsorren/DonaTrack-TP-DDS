package grupo5.donaciones.services.logistica;

import java.util.List;

/**
 * Qué proveedor de logística se prefiere hoy y quién lo puede cambiar sin reiniciar el servicio. El
 * cambio vale para los pedidos que se despachen de ahí en adelante; lo ya encolado en el outbox
 * sigue yendo al proveedor que ya tenía asignado.
 */
public interface IPreferenciaProveedor {

  /** Ids de los proveedores configurados, en el orden de la configuración. */
  List<String> proveedoresConfigurados();

  String proveedorPreferido();

  /**
   * @throws grupo5.common.exceptions.ValidationException si el proveedor no está configurado
   */
  void cambiarProveedorPreferido(String proveedorId);
}
