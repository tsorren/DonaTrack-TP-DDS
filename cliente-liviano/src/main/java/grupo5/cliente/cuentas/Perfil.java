package grupo5.cliente.cuentas;

import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.puertos.PersonaNueva.Direccion;
import grupo5.cliente.puertos.PersonaNueva.Humana;
import grupo5.cliente.puertos.PersonaNueva.Juridica;
import grupo5.cliente.puertos.PersonasDeDonaciones;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que una cuenta nueva completa de su perfil, según quién es; cada uno sabe cómo se registra.
 */
public sealed interface Perfil {

  PersonaNueva persona(String correo);

  /** Lo que se guarda en la cuenta: la persona en cuyo nombre opera. */
  default Map<String, String> atributos(UUID personaId) {
    return Map.of("personaId", personaId.toString());
  }

  /** La plantilla de permisos que recibe de entrada; vacía si espera una aprobación. */
  default Optional<String> plantilla() {
    return Optional.empty();
  }

  default void darDeAlta(PersonasDeDonaciones donaciones, UUID personaId) {}

  /** Quien dona, además de existir como persona, se da de alta como donante. */
  sealed interface Donante extends Perfil {

    @Override
    default void darDeAlta(PersonasDeDonaciones donaciones, UUID personaId) {
      donaciones.registrarDonante(personaId);
    }
  }

  record DonantePersona(String nombre, String apellido) implements Donante {

    @Override
    public PersonaNueva persona(String correo) {
      return new Humana(nombre, apellido, correo);
    }

    @Override
    public Optional<String> plantilla() {
      return Optional.of("DONANTE_PERSONA");
    }
  }

  record DonanteEmpresa(String empresa, String rubro, String nombre) implements Donante {

    @Override
    public PersonaNueva persona(String correo) {
      return new Juridica(
          empresa, "EMPRESA", rubro, null, Humana.deNombreCompleto(nombre, correo), correo);
    }

    @Override
    public Optional<String> plantilla() {
      return Optional.of("RESPONSABLE_DONANTE");
    }
  }

  /** La solicitud de una entidad: queda pendiente hasta que el equipo la apruebe. */
  record Entidad(
      String entidad,
      String queEs,
      String tipo,
      String calle,
      Integer altura,
      String codigoPostal,
      String localidad,
      String provincia,
      String representante)
      implements Perfil {

    private static final Map<String, String> TIPOS_JURIDICOS =
        Map.of("asociacion", "ONG", "institucion", "INSTITUCION", "organismo", "GUBERNAMENTAL");

    @Override
    public PersonaNueva persona(String correo) {
      Direccion direccion =
          new Direccion(calle, altura, codigoPostal, localidad, provincia, "Argentina");
      Humana quienRepresenta = Humana.deNombreCompleto(representante, correo);
      return new Juridica(
          entidad, TIPOS_JURIDICOS.get(tipo), queEs, direccion, quienRepresenta, correo);
    }

    @Override
    public Map<String, String> atributos(UUID personaId) {
      return Map.of("personaId", personaId.toString(), "solicitudEntidad", "pendiente");
    }
  }
}
