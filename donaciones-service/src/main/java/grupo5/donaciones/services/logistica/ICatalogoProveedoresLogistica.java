package grupo5.donaciones.services.logistica;

/** Qué proveedores de logística tienen un adapter registrado para recibir pedidos. */
public interface ICatalogoProveedoresLogistica {

  boolean tieneAdapter(String proveedorId);

  /** Si el adapter de ese proveedor sabe cancelar una entrega (HB-19). */
  boolean admiteCancelacion(String proveedorId);
}
