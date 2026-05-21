package ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class MedioDeContacto {
  private Boolean esPredeterminado;

  public abstract boolean enviarMensaje(String mensaje, NotificacionSender sender);
}
