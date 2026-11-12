package grupo5.logistica.models.entities.entregas;

public enum EstadoEntrega {
  PENDIENTE,
  EN_TRASLADO,
  /** El camión llegó al destino y la entidad todavía no confirmó (HB-22). Es activo. */
  EN_PUERTA,
  ENTREGADA,
  NO_RECIBIDA,
  REVISION,
  /** Se canceló antes de salir porque la necesidad se dio de baja (HB-19). Es final. */
  CANCELADA
}
