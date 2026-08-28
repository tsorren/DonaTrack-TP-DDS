package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class DonanteRegistrado extends EventoNotificable {
  private final String credencialesDeAcceso;

  public DonanteRegistrado(Persona persona, String credencialesDeAcceso, LocalDateTime fecha) {
    super(persona, fecha);
    this.credencialesDeAcceso = credencialesDeAcceso;
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(
            this.getPersona().getId(), "Bienvenido a DonaTrack " + credencialesDeAcceso);

    return List.of(notificacion);
  }
}
