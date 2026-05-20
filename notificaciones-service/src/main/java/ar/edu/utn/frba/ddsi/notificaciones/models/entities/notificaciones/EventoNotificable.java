package ar.edu.utn.frba.ddsi.notificaciones.models.entities.notificaciones;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.persona.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class EventoNotificable {
  private Persona persona;
  private LocalDateTime fecha;

  public abstract List<Notificacion> generarNotificaciones();
}
