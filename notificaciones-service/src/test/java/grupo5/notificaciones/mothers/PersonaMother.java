package grupo5.notificaciones.mothers;

import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/** Object Mother para {@code Persona} (Oleada 8). Nunca sufijo {@code *Test}. */
public final class PersonaMother {

  private PersonaMother() {}

  public static Persona generica() {
    return generica("Juan Perez");
  }

  public static Persona generica(String denominacion) {
    return new Persona(UUID.randomUUID(), new ArrayList<>(), denominacion, TipoPersona.HUMANA);
  }

  /** Persona sin ningún medio de contacto — caso borde de 0 medios. */
  public static Persona sinMedios() {
    return generica();
  }

  /** Persona con exactamente 1 medio de contacto, ya predeterminado. */
  public static Persona conUnMedioPredeterminado() {
    return conMedios(MedioDeContactoMother.correoPredeterminado());
  }

  /** Persona con los medios pasados, agregados en el orden dado (para casos de N medios). */
  public static Persona conMedios(MedioDeContacto... medios) {
    Persona persona = generica();
    Arrays.stream(medios).forEach(persona::agregarMedioDeContacto);
    return persona;
  }
}
