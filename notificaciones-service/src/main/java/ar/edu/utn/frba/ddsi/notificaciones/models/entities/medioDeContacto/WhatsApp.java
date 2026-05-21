package ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto;

public class WhatsApp extends Telefono {
  @Override
  public boolean enviarMensaje(String mensaje, NotificacionSender sender) {
    return sender.enviarA(this, mensaje);
  }
}
