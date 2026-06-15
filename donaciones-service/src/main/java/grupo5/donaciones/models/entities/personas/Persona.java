package grupo5.donaciones.models.entities.personas;

import grupo5.donaciones.models.privacidad.Anonimizable;
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

  public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
    mediosDeContacto.add(medioDeContacto);
  }

  public void quitarMedioDeContacto(MedioDeContacto medioDeContacto) {
    mediosDeContacto.remove(medioDeContacto);
  }

  public void definirMedioDeContactoPredeterminado(MedioDeContacto medioDeContacto) {
    mediosDeContacto.stream()
        .filter(m -> m.getEsPredeterminado() != null && m.getEsPredeterminado())
        .findFirst()
        .ifPresent(m -> m.setEsPredeterminado(false));

    medioDeContacto.setEsPredeterminado(true);
  }
}
