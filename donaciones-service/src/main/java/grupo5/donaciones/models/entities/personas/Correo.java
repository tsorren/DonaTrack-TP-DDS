package grupo5.donaciones.models.entities.personas;

import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Correo extends MedioDeContacto {
  private String direccionCorreo;

  @Override
  public void anonimizar() {
    this.direccionCorreo = Anonimizable.VALOR_STRING;
  }
}
