package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntidadBeneficiaria implements Anonimizable {
  private Juridica juridica;
  private List<Necesidad> necesidades = new ArrayList<>();

  public void agregarNecesidad(Necesidad necesidad) {
    this.necesidades.add(necesidad);
  }

  public void quitarNecesidad(Necesidad necesidad) {

    this.necesidades.remove(necesidad);
  }

  @Override
  public void anonimizar() {
    if (this.juridica != null) {
      this.juridica.anonimizar();
    }
  }
}
