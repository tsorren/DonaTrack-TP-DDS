package grupo5.notificaciones.models.entities.medioDeContacto;

public interface NotificacionSender {
  public boolean enviarA(Correo correo, String mensaje);

  public boolean enviarA(Telefono telefono, String mensaje);

  public boolean enviarA(WhatsApp whatsapp, String mensaje);
}
