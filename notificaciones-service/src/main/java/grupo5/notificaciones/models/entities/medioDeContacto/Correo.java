package grupo5.notificaciones.models.entities.medioDeContacto;

import grupo5.notificaciones.models.entities.persona.Anonimizable;
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
    this.direccionCorreo = Anonimizable.valorString;
  }
}
