package grupo5.donaciones.models.entities.donacionesIndependientes;

public enum TipoEstadoDonacion {
  EN_DEPOSITO,
  ASIGNACION_REALIZADA,
  EN_TRASLADO,
  /** El camión llegó a la entidad y espera su confirmación (HB-22). */
  EN_PUERTA,
  LISTA_PARA_ENTREGAR,
  ENTREGADA,
  ENTREGA_FALLIDA,
  VENCIDA
}
