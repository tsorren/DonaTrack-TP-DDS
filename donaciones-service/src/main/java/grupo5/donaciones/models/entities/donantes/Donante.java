package grupo5.donaciones.models.entities.donantes;

import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Donante implements Anonimizable, AggregateRoot {
  private final UUID id;
  private final UUID personaId;

  public Donante(UUID personaId) {
    if (personaId == null) {
      throw new IllegalArgumentException("El donante debe estar asociado a una persona.");
    }
    this.id = UUID.randomUUID();
    this.personaId = personaId;
  }

  public UUID personaId() {
    return this.personaId;
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  @Override
  public void anonimizar() {
    // Coordinado a nivel de servicio de aplicación (PersonasService)
  }
}
