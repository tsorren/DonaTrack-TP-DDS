package grupo5.notificaciones.models.entities.persona;

import grupo5.notificaciones.models.entities.medioDeContacto.MedioDeContacto;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Persona implements Anonimizable {
  private final List<MedioDeContacto> contactos = new ArrayList<>();
  private String denominacion;
  private TipoPersona tipoPersona;

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

  @Override
  public void anonimizar() {
    this.denominacion = "ANONIMO";
  }
}
