package ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto;

public class WhatsAppSender implements NotificacionSender {
  @Override
  public boolean enviarMensaje(String destino, String mensaje) {
    try {
      System.out.println("Enviando WHATSAPP a " + destino + ": " + mensaje);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
