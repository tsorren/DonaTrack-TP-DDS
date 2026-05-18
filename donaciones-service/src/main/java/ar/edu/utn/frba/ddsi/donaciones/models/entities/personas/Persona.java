package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.medioDeContacto.MedioDeContacto;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Persona {
  private final List<MedioDeContacto> contactos = new ArrayList<>();
  private TipoDocumento tipoDocumento;
  private String documento;
  private Direccion direccion;

  // fuerzo a que se cree con al menos 1 contacto valido
  protected Persona(MedioDeContacto contactoInicial) {
    if (contactoInicial == null) {
      throw new IllegalArgumentException(
          "Una persona debe registrarse con al menos un medio de contacto.");
    }
    contactoInicial.setEsPredeterminado(true);
    this.contactos.add(contactoInicial);
  }

  public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
    if (medioDeContacto == null) {
      throw new IllegalArgumentException("El medio de contacto a agregar no puede ser nulo.");
    }
    contactos.add(medioDeContacto);
  }

  public void quitarMedioDeContacto(MedioDeContacto medioDeContacto) {
    if (!this.contactos.contains(medioDeContacto)) {
      throw new IllegalArgumentException(
          "El medio de contacto a eliminar no pertenece a la persona.");
    }
    if (this.contactos.size() == 1) {
      throw new IllegalStateException(
          "No se puede eliminar el último medio de contacto de una persona.");
    }
    contactos.remove(medioDeContacto);
  }

  public void definirMedioDeContactoPredeterminado(MedioDeContacto medioDeContacto) {
    if (!this.contactos.contains(medioDeContacto)) {
      throw new IllegalArgumentException(
          "El medio de contacto debe pertenecer a la lista para ser predeterminado.");
    }
    this.contactos.forEach(m -> m.setEsPredeterminado(false));
    medioDeContacto.setEsPredeterminado(true);
  }
}
