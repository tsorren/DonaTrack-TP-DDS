package grupo5.logistica.models.entities.entregas;

public enum EstadoEntrega {
  PENDIENTE,
  EN_TRASLADO,
  ENTREGADA,
  NO_RECIBIDA,
  REVISION,
  /** Se canceló antes de salir porque la necesidad se dio de baja (HB-19). Es final. */
  CANCELADA
}
