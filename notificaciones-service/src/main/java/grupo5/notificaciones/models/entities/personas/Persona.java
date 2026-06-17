package grupo5.notificaciones.models.entities.personas;

import grupo5.notificaciones.models.ports.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Persona implements Anonimizable {
  private Long id;
  private final List<MedioDeContacto> mediosDeContacto = new ArrayList<>();
  private String denominacion;
  private TipoPersona tipoPersona;

  public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
    mediosDeContacto.add(medioDeContacto);
  }

  public void quitarMedioDeContacto(MedioDeContacto medioDeContacto) {
    // Ver que pasa si no pertenece a la lista el medio a quitar
    mediosDeContacto.remove(medioDeContacto);
  }

  public void definirMedioDeContactoPredeterminado(MedioDeContacto medioDeContacto) {
    mediosDeContacto.stream()
        .filter(m -> m.getEsPredeterminado() != null && m.getEsPredeterminado())
        .findFirst()
        .ifPresent(m -> m.setEsPredeterminado(false));

    medioDeContacto.setEsPredeterminado(true);
  }

  @Override
  public void anonimizar() {
    this.denominacion = Anonimizable.VALOR_STRING;
    this.mediosDeContacto.forEach(Anonimizable::anonimizar);
  }
}
