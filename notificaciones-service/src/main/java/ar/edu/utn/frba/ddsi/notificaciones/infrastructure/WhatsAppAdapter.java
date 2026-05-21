package ar.edu.utn.frba.ddsi.notificaciones.infrastructure;

public interface WhatsAppAdapter {
  public boolean enviarWhatsApp(String telefono, String mensaje);
}
