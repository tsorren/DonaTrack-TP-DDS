package grupo5.donaciones.models.entities.personas.factories;

import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import java.time.LocalDate;

public class PersonaFactory {

  private PersonaFactory() {
    throw new IllegalStateException("Utility class");
  }

  public static Humana crearHumana(
      String nombre, String apellido, LocalDate fechaNacimiento, Genero genero) {
    return new Humana(nombre, apellido, fechaNacimiento, genero);
  }

  public static Juridica crearJuridica(
      String razonSocial, TipoJuridico tipo, String rubro, Humana representanteInicial) {
    return new Juridica(representanteInicial, razonSocial, tipo, rubro);
  }
}
