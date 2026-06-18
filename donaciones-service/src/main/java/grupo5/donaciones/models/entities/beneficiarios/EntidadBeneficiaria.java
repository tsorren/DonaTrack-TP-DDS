package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntidadBeneficiaria implements Anonimizable, AggregateRoot {
  private final UUID id;
  private Juridica juridica;

  public EntidadBeneficiaria(Juridica juridica) {
    this.id = UUID.randomUUID();
    if (juridica == null) {
      throw new ValidationException(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA);
    }
    this.juridica = juridica;
  }

  @Override
  public void anonimizar() {
    if (this.juridica != null) {
      this.juridica.anonimizar();
    }
  }
}
