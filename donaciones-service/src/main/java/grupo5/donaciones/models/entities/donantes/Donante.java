package grupo5.donaciones.models.entities.donantes;

import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donante implements Anonimizable, AggregateRoot {
  private Persona persona;
  private final UUID id;
  private List<Donacion> donaciones = new ArrayList<>();

  public Donante() {
    this.id = UUID.randomUUID();
  }

  public Donante(Persona persona) {
    this();
    if (persona == null) {
      throw new IllegalArgumentException("El donante debe estar asociado a una persona.");
    }
    this.persona = persona;
  }

  public void agregarDonacion(Donacion donacion) {
    if (donacion != null && !this.donaciones.contains(donacion)) {
      this.donaciones.add(donacion);
      donacion.setDonante(this);
    }
  }

  public void quitarDonacion(Donacion donacion) {
    if (donacion != null && this.donaciones.contains(donacion)) {
      this.donaciones.remove(donacion);
      donacion.setDonante(null);
    }
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  @Override
  public void anonimizar() {
    this.persona.anonimizar();
  }
}
