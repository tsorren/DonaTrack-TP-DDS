package grupo5.notificaciones.infrastructure;

public interface CorreoAdapter {
  public boolean enviarMail(String correo, String mensaje);
}
