package ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Telefono extends MedioDeContacto {
  private String caracteristica;
  private String codigoArea;
  private String numero;

  @Override
  public boolean enviarMensaje(String mensaje, NotificacionSender sender) {
    return sender.enviarMensaje(this.obtenerNumeroCompleto(), mensaje);
  }

  protected String obtenerNumeroCompleto() {
    return caracteristica + codigoArea + numero;
  }
}
