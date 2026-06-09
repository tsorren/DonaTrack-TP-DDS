package grupo5.notificaciones.models.entities.personas;

import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Correo extends MedioDeContacto {
  private String direccionCorreo;

  @Override
  public boolean enviarMensaje(String mensaje, NotificacionSender sender) {
    return sender.enviarA(this, mensaje);
  }

  @Override
  public void anonimizar() {
    this.direccionCorreo = Anonimizable.VALOR_STRING;
  }
}
