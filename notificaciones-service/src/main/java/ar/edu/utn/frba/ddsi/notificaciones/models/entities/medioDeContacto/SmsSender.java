package ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto;

public class SmsSender implements NotificacionSender {
  @Override
  public boolean enviarMensaje(String destino, String mensaje) {
    try {
      System.out.println("Enviando SMS a " + destino + ": " + mensaje);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
