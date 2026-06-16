package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonanteRegistrado extends EventoNotificable {
  private String credencialesDeAcceso;

  public DonanteRegistrado(Persona persona, String credencialesDeAcceso, LocalDateTime fecha) {
    super();
    this.credencialesDeAcceso = credencialesDeAcceso;
  }

  public DonanteRegistrado() {}

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(this.getPersona(), "Bienvenido a DonaTrack\n" + credencialesDeAcceso);

    return List.of(notificacion);
  }
}
