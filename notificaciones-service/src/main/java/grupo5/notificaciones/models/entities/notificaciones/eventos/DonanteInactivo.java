package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonanteInactivo extends EventoNotificable {
  private Integer diasInactividad;

  public DonanteInactivo(Persona persona, Integer diasInactividad, LocalDateTime fecha) {
    this.setPersona(persona);
    this.diasInactividad = diasInactividad;
    this.setFecha(fecha);
  }

  public DonanteInactivo() {}

  @Override
  public List<Notificacion> generarNotificaciones() {
    // Armamos un mensaje empático que fomente el regreso del donante
    String mensaje =
        "¡Te extrañamos en DonaTrack! "
            + "Hemos notado que han pasado "
            + diasInactividad
            + " días desde tu última actividad. "
            + "Las entidades beneficiarias aún necesitan tu ayuda. ¡Vuelve a hacer la diferencia!";

    Notificacion notificacion = new Notificacion(this.getPersona(), mensaje);

    return List.of(notificacion);
  }
}
