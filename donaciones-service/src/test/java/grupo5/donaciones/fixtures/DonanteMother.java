package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;

public final class DonanteMother {

  private DonanteMother() {}

  public static Donante juanPerez() {
    return new Donante(PersonaMother.juanPerez().getId());
  }

  public static Donante mariaGomez() {
    return new Donante(PersonaMother.mariaGomez().getId());
  }

  public static Donante paraPersona(Persona persona) {
    return new Donante(persona.getId());
  }
}
