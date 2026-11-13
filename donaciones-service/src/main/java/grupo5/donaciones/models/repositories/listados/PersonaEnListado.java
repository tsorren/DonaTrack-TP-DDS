package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
import java.util.Comparator;
import java.util.Locale;

/**
 * Las reglas de búsqueda y orden de una persona en los listados en memoria; las consultas SQL de
 * los adaptadores JPA hacen lo mismo.
 */
public final class PersonaEnListado {

  /** Por nombre visible sin tildes ni mayúsculas; desempata por id. */
  public static final Comparator<Persona> POR_NOMBRE =
      Comparator.comparing((Persona p) -> TextoBuscado.normalizar(nombreVisible(p)))
          .thenComparing(Persona::getId);

  private PersonaEnListado() {}

  /** La razón social de la jurídica, o el apellido y el nombre de la humana. */
  public static String nombreVisible(Persona persona) {
    return switch (persona) {
      case Juridica j -> j.getRazonSocial();
      case Humana h -> h.getApellido() + " " + h.getNombre();
    };
  }

  /** Nombre, apellido, razón social o número de documento contienen {@code q}. */
  public static boolean coincide(Persona persona, String q) {
    if (q == null) {
      return true;
    }
    if (TextoBuscado.contiene(persona.getDocumento(), q)) {
      return true;
    }
    return switch (persona) {
      case Juridica j -> TextoBuscado.contiene(j.getRazonSocial(), q);
      case Humana h ->
          TextoBuscado.contiene(h.getNombre(), q) || TextoBuscado.contiene(h.getApellido(), q);
    };
  }

  /** Del tipo pedido; {@code tipoJuridico} solo deja pasar jurídicas de ese tipo. */
  public static boolean esDelTipo(Persona persona, TipoPersona tipo, TipoJuridico tipoJuridico) {
    if (tipo != null && persona.getTipoPersona() != tipo) {
      return false;
    }
    return tipoJuridico == null || (persona instanceof Juridica j && j.getTipo() == tipoJuridico);
  }

  /** Tiene un medio de contacto del canal: CORREO, TELEFONO o WHATSAPP. */
  public static boolean tieneCanal(Persona persona, String canal) {
    String buscado = canal.toUpperCase(Locale.ROOT);
    return persona.getMediosDeContacto().stream()
        .anyMatch(
            medio ->
                switch (medio) {
                  case Correo c -> "CORREO".equals(buscado);
                  case Telefono t ->
                      t.getTipo() == TipoTelefono.WHATSAPP
                          ? "WHATSAPP".equals(buscado)
                          : "TELEFONO".equals(buscado);
                  default -> false;
                });
  }
}
