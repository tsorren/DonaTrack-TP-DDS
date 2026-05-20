package ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto;

public interface NotificacionSender {
  public boolean enviarMensaje(String destino, String mensaje);
}
