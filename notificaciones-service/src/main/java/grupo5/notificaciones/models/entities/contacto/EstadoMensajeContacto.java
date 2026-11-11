package grupo5.notificaciones.models.entities.contacto;

/** RECIBIDO pasa a RESPONDIDO, o a ENVIO_FALLIDO si el correo no sale (desde ahí se reintenta). */
public enum EstadoMensajeContacto {
  RECIBIDO,
  RESPONDIDO,
  ENVIO_FALLIDO
}
