package grupo5.notificaciones.models.ports;

import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.WhatsApp;

public interface NotificacionSender {
  public boolean enviarA(Correo correo, String mensaje);

  public boolean enviarA(Telefono telefono, String mensaje);

  public boolean enviarA(WhatsApp whatsapp, String mensaje);
}
