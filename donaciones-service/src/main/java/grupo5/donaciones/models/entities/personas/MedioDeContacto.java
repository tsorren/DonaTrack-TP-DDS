package grupo5.donaciones.models.entities.personas;

import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class MedioDeContacto implements Anonimizable {
  private Boolean esPredeterminado;

  protected MedioDeContacto() {
    this.esPredeterminado = false;
  }
}
