package grupo5.donaciones.models.entities.donantes;

import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donante implements Anonimizable {
  private Persona persona;

  public Donante(Persona persona) {

    if (persona == null) {
      throw new IllegalArgumentException("El donante debe estar asociado a una persona.");
    }
    this.persona = persona;
  }

  @Override
  public void anonimizar() {
    this.persona.anonimizar();
  }
}
