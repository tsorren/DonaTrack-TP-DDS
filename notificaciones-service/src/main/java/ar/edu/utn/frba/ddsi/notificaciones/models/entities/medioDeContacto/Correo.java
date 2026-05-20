package ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Correo extends MedioDeContacto {
  private String direccionCorreo;

  @Override
  public boolean enviarMensaje(String mensaje, NotificacionSender sender) {
    return sender.enviarMensaje(this.direccionCorreo, mensaje);
  }
}
