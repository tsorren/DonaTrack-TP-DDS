package grupo5.notificaciones.models.entities.personas;

import grupo5.common.repositories.AggregateRoot;
import grupo5.notificaciones.models.ports.Anonimizable;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Persona implements Anonimizable, AggregateRoot {
  private final UUID id;

  @Getter(AccessLevel.NONE)
  private final List<MedioDeContacto> mediosDeContacto;

  public List<MedioDeContacto> getMediosDeContacto() {
    return List.copyOf(this.mediosDeContacto);
  }

  private String denominacion;
  private TipoPersona tipoPersona;

  @Override
  public UUID getId() {
    return this.id;
  }

  public Persona(
      UUID id,
      List<MedioDeContacto> mediosDeContacto,
      String denominacion,
      TipoPersona tipoPersona) {
    this.id = id;
    this.mediosDeContacto = mediosDeContacto;
    this.denominacion = denominacion;
    this.tipoPersona = tipoPersona;
  }

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
        .ifPresent(MedioDeContacto::desmarcarComoPredeterminado);

    medioDeContacto.marcarComoPredeterminado();
  }

  @Override
  public void anonimizar() {
    this.denominacion = Anonimizable.VALOR_STRING;
    this.mediosDeContacto.forEach(Anonimizable::anonimizar);
  }
}
