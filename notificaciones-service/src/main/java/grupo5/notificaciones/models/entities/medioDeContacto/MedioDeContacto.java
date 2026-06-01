package grupo5.notificaciones.models.entities.medioDeContacto;

import grupo5.notificaciones.models.entities.persona.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class MedioDeContacto implements Anonimizable {
  private Boolean esPredeterminado;

  public MedioDeContacto() {
    this.esPredeterminado = false;
  }

  public abstract boolean enviarMensaje(String mensaje, NotificacionSender sender);

  public abstract void anonimizar();
}
