package grupo5.notificaciones.infrastructure;

public interface TelefonoAdapter {
  public boolean enviarSms(String telefono, String mensaje);
}
