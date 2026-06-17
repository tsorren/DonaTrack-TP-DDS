package grupo5.donaciones.models.entities.personas;

import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// El modificador "sealed" permite pattern matching del paradigma funcional dentro de un switch
public abstract sealed class Persona implements Anonimizable, AggregateRoot
    permits Humana, Juridica {
  private final UUID id;
  private TipoDocumento tipoDocumento;
  private String documento;
  private Direccion direccion;
  private List<MedioDeContacto> mediosDeContacto;

  // fuerzo a que se cree con al menos 1 contacto valido
  protected Persona() {
    this.id = UUID.randomUUID(); // Generamos el id porque somos owners del dato
    this.mediosDeContacto = new ArrayList<>();
  }

  public abstract TipoPersona getTipoPersona();

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
