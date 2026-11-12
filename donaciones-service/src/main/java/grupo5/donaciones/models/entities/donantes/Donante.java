package grupo5.donaciones.models.entities.donantes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** Rol de donante. Su identidad es la de la {@link Persona}: {@code id == personaId}. */
public class Donante implements Anonimizable, AggregateRoot {
  private final UUID id;
  private boolean activo;
  private boolean mostrarNombre = true;
  // La del primer registro como donante: una reactivación no la cambia (R11-01)
  private LocalDateTime fechaAlta;

  public Donante(UUID personaId) {
    if (personaId == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_SIN_PERSONA);
    }
    this.id = personaId;
    this.activo = true;
    this.fechaAlta = LocalDateTime.now(ZoneId.systemDefault());
  }

  /** Reconstitución con la fecha de alta guardada (adaptador JPA). */
  public Donante(UUID personaId, boolean activo, boolean mostrarNombre, LocalDateTime fechaAlta) {
    this(personaId, activo, mostrarNombre);
    if (fechaAlta != null) {
      this.fechaAlta = fechaAlta;
    }
  }

  public LocalDateTime getFechaAlta() {
    return this.fechaAlta;
  }

  public Donante(Persona persona) {
    this(persona.getId());
  }

  /** Constructor de reconstitución para el adaptador JPA */
  public Donante(UUID personaId, boolean activo) {
    this(personaId, activo, true);
  }

  public Donante(UUID personaId, boolean activo, boolean mostrarNombre) {
    this(personaId);
    this.activo = activo;
    this.mostrarNombre = mostrarNombre;
  }

  /**
   * Si la entidad que recibe sus donaciones ve su nombre (HB-20). Vale también para las pasadas.
   */
  public boolean muestraNombre() {
    return this.mostrarNombre;
  }

  public void mostrarNombre(boolean mostrar) {
    this.mostrarNombre = mostrar;
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
