package grupo5.donaciones.dto.logistica;

/** Qué informa un proveedor de logística por el callback HTTP. */
public enum TipoAvisoProveedor {
  RUTA_ASIGNADA,
  RUTA_INICIADA,
  ENTREGA_EXITOSA,
  ENTREGA_FALLIDA
}
