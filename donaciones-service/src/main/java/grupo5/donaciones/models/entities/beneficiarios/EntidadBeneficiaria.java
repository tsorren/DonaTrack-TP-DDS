package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntidadBeneficiaria implements Anonimizable {
  private Juridica juridica;
  private List<Necesidad> necesidades;

  public EntidadBeneficiaria(Juridica juridica) {
    if (juridica == null) {
      throw new ValidationException(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA);
    }
    this.juridica = juridica;
    this.necesidades = new ArrayList<>();
  }

  public void agregarNecesidad(Necesidad necesidad) {
    if (necesidad == null) {
      throw new ValidationException(ErrorCatalog.AGREGAR_NECESIDAD_NULA);
    }

    if (this.necesidades.contains(necesidad)) {
      throw new ValidationException(ErrorCatalog.NECESIDAD_YA_REGISTRADA);
    }

    this.necesidades.add(necesidad);
  }

  public void quitarNecesidad(Necesidad necesidad) {

    if (!this.necesidades.contains(necesidad)) {
      throw new ValidationException(ErrorCatalog.NECESIDAD_NO_PERTENECE_A_ENTIDAD);
    }

    this.necesidades.remove(necesidad);
  }

  @Override
  public void anonimizar() {
    if (this.juridica != null) {
      this.juridica.anonimizar();
    }
  }
}
