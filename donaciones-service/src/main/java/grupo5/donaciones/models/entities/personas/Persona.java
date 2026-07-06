package grupo5.donaciones.models.entities.personas;

import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public abstract sealed class Persona implements Anonimizable, AggregateRoot
    permits Humana, Juridica {
  private final UUID id;
  private TipoDocumento tipoDocumento;
  private String documento;
  private Direccion direccion;

  @Getter(AccessLevel.NONE)
  private List<MedioDeContacto> mediosDeContacto;

  protected Persona() {
    this.id = UUID.randomUUID();
    this.mediosDeContacto = new ArrayList<>();
    this.mediosDeContacto.add(new Telefono());
  }

  /** Permite fijar el id explícitamente (uso: seeding de entidades con id conocido). */
  protected Persona(UUID id) {
    this.id = id;
    this.mediosDeContacto = new ArrayList<>();
    this.mediosDeContacto.add(new Telefono());
  }

  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(mediosDeContacto);
  }

  public void actualizarDocumento(TipoDocumento tipo, String documento) {
    this.tipoDocumento = tipo;
    this.documento = documento;
  }

  public void actualizarDireccion(Direccion direccion) {
    this.direccion = direccion;
  }

  public abstract TipoPersona getTipoPersona();

  public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
    if (medioDeContacto != null) {
      mediosDeContacto.add(medioDeContacto);
    }
  }

  public void limpiarMediosDeContacto() {
    this.mediosDeContacto.clear();
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
