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
  private List<Necesidad> necesidades;

  public EntidadBeneficiaria(Juridica juridica) {
    if (juridica == null) {
      throw new IllegalArgumentException(
          "La entidad beneficiaria debe estar asociada a una persona jurídica.");
    }
    this.juridica = juridica;
    this.necesidades = new ArrayList<>();
  }

  public void agregarNecesidad(Necesidad necesidad) {
    if (necesidad == null) {
      throw new IllegalArgumentException("La necesidad a agregar no puede ser nula.");
    }

    if (this.necesidades.contains(necesidad)) {
      throw new IllegalArgumentException("La necesidad ya se encuentra registrada.");
    }

    this.necesidades.add(necesidad);
  }

  public void quitarNecesidad(Necesidad necesidad) {

    if (!this.necesidades.contains(necesidad)) {
      throw new IllegalArgumentException("La necesidad no pertenece a la entidad beneficiaria.");
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
