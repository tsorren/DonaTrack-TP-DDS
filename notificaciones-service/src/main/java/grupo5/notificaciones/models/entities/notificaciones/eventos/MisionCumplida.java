package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;

import java.time.LocalDateTime;
import java.util.List;

import grupo5.notificaciones.models.entities.persona.Persona;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionCumplida extends EventoNotificable {
  private String nombreMision;
  private String recompensa;

    public MisionCumplida(Persona persona, String nombreMision, String recompensa, LocalDateTime fecha) {
        super();
        this.nombreMision = nombreMision;
        this.recompensa = recompensa;
    }

    public MisionCumplida() {

    }

    @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(
            this.getPersona(),
            "Completaste la misión " + nombreMision + ". Recompensa: " + recompensa);

    return List.of(notificacion);
  }
}
