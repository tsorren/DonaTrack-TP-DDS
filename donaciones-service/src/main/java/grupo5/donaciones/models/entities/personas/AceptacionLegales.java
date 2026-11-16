package grupo5.donaciones.models.entities.personas;

import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Constancia de que una persona aceptó una versión de los legales (HB-20). No guarda IP ni
 * navegador, y sobrevive a la anonimización porque la persona conserva su id.
 */
public record AceptacionLegales(UUID id, UUID personaId, int version, LocalDateTime aceptadaEn)
    implements AggregateRoot {

  public AceptacionLegales(UUID personaId, int version, LocalDateTime aceptadaEn) {
    this(UUID.randomUUID(), personaId, version, aceptadaEn);
  }

  @Override
  public UUID getId() {
    return id;
  }
}
