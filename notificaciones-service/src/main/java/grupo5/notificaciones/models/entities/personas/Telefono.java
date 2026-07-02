package grupo5.notificaciones.models.entities.personas;

import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Telefono extends MedioDeContacto {
  private String caracteristica;
  private String codigoArea;
  private String numero;
  private TipoTelefono tipo = TipoTelefono.ESTANDAR;

  @Override
  public boolean enviarMensaje(String mensaje, NotificacionSender sender) {
    return sender.enviarA(this, mensaje);
  }

  public String obtenerNumeroCompleto() {
    return this.caracteristica + this.codigoArea + this.numero;
  }

  @Override
  public void anonimizar() {
    this.caracteristica = Anonimizable.VALOR_STRING;
    this.codigoArea = Anonimizable.VALOR_STRING;
    this.numero = Anonimizable.VALOR_STRING;
  }
}
