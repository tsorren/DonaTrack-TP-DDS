package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class DonanteInactivo extends EventoNotificable {
  private final Integer diasInactividad;

  public DonanteInactivo(Persona persona, Integer diasInactividad, LocalDateTime fecha) {
    super(persona, fecha);
    this.diasInactividad = diasInactividad;
  }

  @Override
  public List<Notificacion> generarNotificaciones() {

    String mensaje =
        "¡Te extrañamos en DonaTrack! "
            + "Hemos notado que han pasado "
            + diasInactividad
            + " días desde tu última actividad. "
            + "Las entidades beneficiarias aún necesitan tu ayuda. ¡Vuelve a hacer la diferencia!";

    Notificacion notificacion = new Notificacion(this.getPersona().getId(), mensaje);

    return List.of(notificacion);
  }
}
