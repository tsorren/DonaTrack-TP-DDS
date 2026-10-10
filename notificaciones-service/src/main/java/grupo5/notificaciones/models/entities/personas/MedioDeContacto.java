package grupo5.notificaciones.models.entities.personas;

import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import lombok.Getter;

@Getter
public abstract class MedioDeContacto implements Anonimizable {
  private Boolean esPredeterminado;

  protected MedioDeContacto() {
    this.esPredeterminado = false;
  }

  public void marcarComoPredeterminado() {
    this.esPredeterminado = true;
  }

  public void desmarcarComoPredeterminado() {
    this.esPredeterminado = false;
  }

  public abstract boolean enviarMensaje(String mensaje, NotificacionSender sender);
}
