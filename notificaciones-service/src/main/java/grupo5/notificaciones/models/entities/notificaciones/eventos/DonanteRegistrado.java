package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;

import java.time.LocalDateTime;
import java.util.List;

import grupo5.notificaciones.models.entities.persona.Persona;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonanteRegistrado extends EventoNotificable {
  private String credencialesDeAcceso;

    public DonanteRegistrado(Persona persona, String credencialesDeAcceso, LocalDateTime fecha) {
        super();
        credencialesDeAcceso = credencialesDeAcceso;
    }

    public DonanteRegistrado() {

    }

    @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(this.getPersona(), "Has sido registrado en el sistema con las siguientes credenciales" + "\n" + credencialesDeAcceso);

    return List.of(notificacion);
  }
}
