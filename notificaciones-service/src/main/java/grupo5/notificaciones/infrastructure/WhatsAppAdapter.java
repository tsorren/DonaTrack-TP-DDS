package grupo5.notificaciones.infrastructure;

public interface WhatsAppAdapter {
  public boolean enviarWhatsApp(String telefono, String mensaje);
}
