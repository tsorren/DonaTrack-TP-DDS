package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.UUID;
import lombok.Getter;

@Getter
public class EntidadBeneficiaria implements Anonimizable, AggregateRoot {
  private final UUID id;
  private final UUID juridicaId;

  public EntidadBeneficiaria(UUID juridicaId) {
    this(UUID.randomUUID(), juridicaId);
  }

  public EntidadBeneficiaria(UUID id, UUID juridicaId) {
    if (juridicaId == null) {
      throw new ValidationException(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA);
    }
    this.id = id != null ? id : UUID.randomUUID();
    this.juridicaId = juridicaId;
  }

  public UUID juridicaId() {
    return this.juridicaId;
  }

  @Override
  public void anonimizar() {
    // Coordinado a nivel de servicio de aplicación (PersonasService)
  }
}
