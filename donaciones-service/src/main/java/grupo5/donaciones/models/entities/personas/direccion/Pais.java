package grupo5.donaciones.models.entities.personas.direccion;

import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Pais implements Anonimizable {
  private String nombre;

  @Override
  public void anonimizar() {
    this.nombre = Anonimizable.VALOR_STRING;
  }
}
