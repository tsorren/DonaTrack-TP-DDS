package grupo5.donaciones.models.entities.personas;

import grupo5.donaciones.models.entities.personas.direccion.Direccion;
import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Persona implements Anonimizable {
  private Long id;
  private TipoDocumento tipoDocumento;
  private String documento;
  private Direccion direccion;

  // fuerzo a que se cree con al menos 1 contacto valido
  protected Persona() {}
}
