package grupo5.donaciones.dto.actividad;

import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.models.entities.actividad.Actividad;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una acción del registro de actividad (R11-26): {@code ocurrioEn} en la hora local de Argentina,
 * sin zona, como {@code fechaAlta}; {@code autor}, el {@code sub} de quien la hizo; {@code
 * objetoId} y {@code detalle}, nullable.
 */
public record ActividadDTO(
    UUID id,
    LocalDateTime ocurrioEn,
    String autor,
    AccionDeAdministracion accion,
    String objetoId,
    String detalle) {

  public static ActividadDTO de(Actividad actividad) {
    return new ActividadDTO(
        actividad.id(),
        actividad.ocurrioEn(),
        actividad.autor(),
        actividad.accion(),
        actividad.objetoId(),
        actividad.detalle());
  }
}
