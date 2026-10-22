package grupo5.donaciones.models.entities.donantes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.UUID;

/** Rol de donante. Su identidad es la de la {@link Persona}: {@code id == personaId}. */
public class Donante implements Anonimizable, AggregateRoot {
  private final UUID id;
  private boolean activo;

  public Donante(UUID personaId) {
    if (personaId == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_SIN_PERSONA);
    }
    this.id = personaId;
    this.activo = true;
  }

  public Donante(Persona persona) {
    this(persona.getId());
  }

  public UUID personaId() {
    return this.id;
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  public boolean estaActivo() {
    return this.activo;
  }

  /** Devuelve {@code true} solo si hubo transición de activo a inactivo. */
  public boolean darDeBaja() {
    boolean estabaActivo = this.activo;
    this.activo = false;
    return estabaActivo;
  }

  /** Devuelve {@code true} solo si hubo transición de inactivo a activo. */
  public boolean reactivar() {
    boolean estabaInactivo = !this.activo;
    this.activo = true;
    return estabaInactivo;
  }

  @Override
  public void anonimizar() {
    // Coordinado a nivel de servicio de aplicación (PersonasService)
  }
}
