package grupo5.notificaciones.models.entities.medioDeContacto;

import grupo5.notificaciones.models.entities.persona.Anonimizable;
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
    return sender.enviarA(this, mensaje);
  }

  public String obtenerNumeroCompleto() {
    return this.caracteristica + this.codigoArea + this.numero;
  }

  @Override
  public void anonimizar() {
    this.caracteristica = Anonimizable.valorString;
    this.codigoArea = Anonimizable.valorString;
    this.numero = Anonimizable.valorString;
  }
}
