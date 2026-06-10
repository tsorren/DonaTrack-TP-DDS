package grupo5.donaciones.models.entities.personas;

import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Localidad implements Anonimizable {
  private String nombre;
  private Provincia provincia;

  @Override
  public void anonimizar() {
    this.nombre = Anonimizable.VALOR_STRING;
    this.provincia.anonimizar();
  }
}
