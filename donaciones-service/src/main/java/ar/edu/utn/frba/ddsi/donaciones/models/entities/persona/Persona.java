package ar.edu.utn.frba.ddsi.donaciones.models.entities.persona;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.persona.medioDeContacto.MedioDeContacto;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
abstract class Persona {
  private final List<MedioDeContacto> contactos;
  private TipoDocumento tipoDocumento;
  private String documento;
  private Direccion direccion;

  public Persona() {
    this.contactos = new ArrayList<>();
  }

  public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
    contactos.add(medioDeContacto);
  }

  public void quitarMedioDeContacto(MedioDeContacto medioDeContacto) {
    // Ver que pasa si no pertenece a la lista el medio a quitar
    contactos.remove(medioDeContacto);
  }

  public void definirMedioDeContactoPredeterminado(MedioDeContacto medioDeContacto) {
    contactos.stream()
        .filter(MedioDeContacto::getEsPredeterminado)
        .findFirst()
        .ifPresent(m -> m.setEsPredeterminado(false));

    medioDeContacto.setEsPredeterminado(true);
  }
}
