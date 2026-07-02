package grupo5.notificaciones.models.ports;

import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Telefono;

public interface NotificacionSender {
  public boolean enviarA(Correo correo, String mensaje);

  public boolean enviarA(Telefono telefono, String mensaje);
}
