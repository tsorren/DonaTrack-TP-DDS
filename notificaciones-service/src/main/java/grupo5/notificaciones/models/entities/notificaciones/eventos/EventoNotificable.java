package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public abstract class EventoNotificable {
  private Persona persona;
  private LocalDateTime fecha;

  public abstract List<Notificacion> generarNotificaciones();
}
