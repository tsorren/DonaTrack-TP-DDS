package grupo5.cliente.vistas;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * La solicitud de la organización que espera la aprobación (R11-41), con los nombres del formulario
 * del registro: {@code entidad} es el nombre; {@code queEs}, el rubro; {@code tipo}, la opción de
 * Tipo de organización ({@code asociacion}, {@code institucion} u {@code organismo}); {@code
 * representante}, el nombre y el apellido juntos; {@code correo}, el de la cuenta (solo de
 * lectura). Lo que no se sabe, null.
 */
public record SolicitudEntidadVista(
    String entidad,
    String queEs,
    String tipo,
    String calle,
    Integer altura,
    String codigoPostal,
    String localidad,
    String provincia,
    String representante,
    String correo) {

  private static final Map<String, String> TIPOS =
      Map.of(
          "asociacion", "Asociación civil u ONG",
          "institucion", "Institución: escuela, club o biblioteca",
          "organismo", "Organismo público");

  /** El tipo como lo dice la opción del registro ("Asociación civil u ONG"), no el código. */
  public String tipoTexto() {
    return tipo == null ? null : TIPOS.get(tipo);
  }

  /** La dirección en un renglón: "Calle Ejemplo 1234, Localidad, Provincia · CP 1000". */
  public String direccion() {
    String calleYAltura = unidos(" ", calle, altura == null ? null : altura.toString());
    String lugar = unidos(", ", calleYAltura, localidad, provincia);
    return codigoPostal == null || codigoPostal.isBlank()
        ? lugar
        : unidos(" · ", lugar, "CP " + codigoPostal);
  }

  private static String unidos(String separador, String... partes) {
    return Stream.of(partes)
        .filter(Objects::nonNull)
        .filter(parte -> !parte.isBlank())
        .collect(Collectors.joining(separador));
  }
}
