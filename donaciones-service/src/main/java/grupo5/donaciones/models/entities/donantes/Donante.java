package grupo5.donaciones.models.entities.donantes;

import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donante implements Anonimizable, AggregateRoot {
  private Persona persona;
  private final UUID id;

  public Donante() {
    this.id = UUID.randomUUID();
  }

  public Donante(Persona persona) {
    this();
    if (persona == null) {
      throw new IllegalArgumentException("El donante debe estar asociado a una persona.");
    }
    this.persona = persona;
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  @Override
  public void anonimizar() {
    this.persona.anonimizar();
  }
}
