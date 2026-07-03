package grupo5.donaciones.models.entities.personas.factories;

import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import java.time.LocalDate;
import java.util.UUID;

public class PersonaFactory {

  private PersonaFactory() {
    throw new IllegalStateException("Utility class");
  }

  public static Humana crearHumana(
      String nombre, String apellido, LocalDate fechaNacimiento, Genero genero) {
    return new Humana(nombre, apellido, fechaNacimiento, genero);
  }

  /** Crea una Humana con id fijo. Uso exclusivo de seeding (para persona admin). */
  public static Humana crearHumanaConId(
      UUID id, String nombre, String apellido, LocalDate fechaNacimiento, Genero genero) {
    return new Humana(id, nombre, apellido, fechaNacimiento, genero);
  }

  public static Juridica crearJuridica(
      String razonSocial, TipoJuridico tipo, String rubro, Humana representanteInicial) {
    return new Juridica(representanteInicial, razonSocial, tipo, rubro);
  }
}
