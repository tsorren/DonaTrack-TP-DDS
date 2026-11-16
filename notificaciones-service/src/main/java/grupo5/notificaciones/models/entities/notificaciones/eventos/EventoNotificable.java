package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.TipoEvento;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class EventoNotificable {
  private final Persona persona;
  private final LocalDateTime fecha;

  /** La donación a la que se refiere el evento, si el emisor la informa (HB-09). */
  private UUID referenciaId;

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

  public abstract TipoEvento tipoEvento();

  protected abstract List<Notificacion> crearNotificaciones();

  public void asociarReferencia(UUID referenciaId) {
    this.referenciaId = referenciaId;
  }

  /** Las notificaciones del evento, cada una con su tipo y la referencia (HB-09). */
  public final List<Notificacion> generarNotificaciones() {
    List<Notificacion> notificaciones = crearNotificaciones();
    notificaciones.forEach(n -> n.etiquetar(tipoEvento().name(), referenciaId));
    return notificaciones;
  }
}
