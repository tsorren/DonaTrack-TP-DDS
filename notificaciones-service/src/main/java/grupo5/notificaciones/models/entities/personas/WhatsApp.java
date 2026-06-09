package grupo5.notificaciones.models.entities.personas;

import grupo5.notificaciones.models.ports.NotificacionSender;

public class WhatsApp extends Telefono {
  @Override
  public boolean enviarMensaje(String mensaje, NotificacionSender sender) {
    return sender.enviarA(this, mensaje);
  }
}
