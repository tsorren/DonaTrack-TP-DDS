package grupo5.donaciones.models.entities.donantes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Donante implements Anonimizable, AggregateRoot {
  private final UUID id;
  private final UUID personaId;

  public Donante(UUID personaId) {
    this(UUID.randomUUID(), personaId);
  }

  public Donante(UUID id, UUID personaId) {
    if (personaId == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_SIN_PERSONA);
    }
    if (id == null) {
      throw new IllegalArgumentException("El id del donante no puede ser nulo");
    }
    this.id = id;
    this.personaId = personaId;
  }

  public Donante(Persona persona) {
    this(persona.getId());
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
