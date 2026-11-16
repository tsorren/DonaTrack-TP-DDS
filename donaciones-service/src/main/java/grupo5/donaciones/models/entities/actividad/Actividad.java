package grupo5.donaciones.models.entities.actividad;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Una acción de administración ya hecha (R11-26): quién ({@code autor}, el {@code sub} del token),
 * qué, sobre qué ({@code objetoId}, nullable) y cuándo, con un rótulo sin datos personales ({@code
 * detalle}, nullable, recortado a {@link #LARGO_DETALLE}). Las filas no se editan ni se borran.
 */
public record Actividad(
    UUID id,
    LocalDateTime ocurrioEn,
    String autor,
    AccionDeAdministracion accion,
    String objetoId,
    String detalle) {

  public static final int LARGO_DETALLE = 200;

  public Actividad {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(ocurrioEn, "ocurrioEn");
    Objects.requireNonNull(autor, "autor");
    Objects.requireNonNull(accion, "accion");
    detalle = recortado(detalle);
  }

  /** Una nueva, con id propio. */
  public static Actividad nueva(
      LocalDateTime ocurrioEn,
      String autor,
      AccionDeAdministracion accion,
      String objetoId,
      String detalle) {
    return new Actividad(UUID.randomUUID(), ocurrioEn, autor, accion, objetoId, detalle);
  }

  private static String recortado(String detalle) {
    if (detalle == null || detalle.isBlank()) {
      return null;
    }
    String limpio = detalle.strip();
    return limpio.length() > LARGO_DETALLE ? limpio.substring(0, LARGO_DETALLE) : limpio;
  }
}
