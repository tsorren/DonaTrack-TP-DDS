package grupo5.donaciones.services.logistica;

/** Qué pasó al pedirle al broker que cancele la entrega de una donación (HB-19). */
public enum ResultadoPedidoCancelacion {
  /** Se mandará el comando: la donación espera la confirmación del proveedor. */
  PEDIDA,
  /** Ningún proveedor tiene la entrega (no hay solicitud activa): se libera en el acto. */
  SIN_ENTREGA,
  /** El proveedor no sabe cancelar: la donación sigue su camino. */
  NO_SOPORTADA
}
