package grupo5.donaciones.services.logistica;

/** Resultado de un intento de envío de un pedido de entrega a un proveedor de logística. */
public enum ResultadoEnvio {
  /** El proveedor recibió el pedido. */
  PUBLICADO,
  /** Es seguro que el pedido no llegó: se puede probar con otro proveedor. */
  RECHAZADO,
  /** No se sabe si el pedido llegó: solo se reintenta con el mismo proveedor. */
  INCIERTO,
  /** El proveedor rechazó el contenido del pedido: no se reintenta ni se reenvía. */
  ERROR_CONTRATO
}
