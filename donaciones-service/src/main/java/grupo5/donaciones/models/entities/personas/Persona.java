package grupo5.donaciones.models.entities.personas;

import grupo5.donaciones.models.privacidad.Anonimizable;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Persona implements Anonimizable {
  private Long id;
  private TipoDocumento tipoDocumento;
  private String documento;
  private Direccion direccion;
  private List<MedioDeContacto> mediosDeContacto;

  // fuerzo a que se cree con al menos 1 contacto valido
  protected Persona() {
    this.mediosDeContacto = new ArrayList<>();
  }
}
