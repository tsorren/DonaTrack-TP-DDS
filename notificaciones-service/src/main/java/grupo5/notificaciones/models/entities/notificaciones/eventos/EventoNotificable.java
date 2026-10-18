package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public abstract class EventoNotificable {
  private final Persona persona;
  private final LocalDateTime fecha;

  // RF-06 (Oleada 3): antes cada subclase llamaba this.setPersona(...)/this.setFecha(...) desde
  // su propio constructor sobre @Setter públicos heredados. Ahora se pasa por super(...), con
  // guardas de obligatoriedad acá en el único lugar donde se puede validar una sola vez.
  protected EventoNotificable(Persona persona, LocalDateTime fecha) {
    if (persona == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (fecha == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.persona = persona;
    this.fecha = fecha;
  }

  public abstract List<Notificacion> generarNotificaciones();
}
