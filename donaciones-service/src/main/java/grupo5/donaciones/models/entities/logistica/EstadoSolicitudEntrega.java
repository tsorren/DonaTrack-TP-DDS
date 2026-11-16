package grupo5.donaciones.models.entities.logistica;

public enum EstadoSolicitudEntrega {
  PENDIENTE,
  ENVIADA,
  FALLIDA,
  /** Logística confirmó la cancelación (HB-19): la solicitud se cierra. */
  CANCELADA
}
