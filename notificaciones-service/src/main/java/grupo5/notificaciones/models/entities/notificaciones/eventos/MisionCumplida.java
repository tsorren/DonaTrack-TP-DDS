package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionCumplida extends EventoNotificable {
  private String nombreMision;
  private String recompensa;

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(
            this.getPersona(),
            "Completaste la misión " + nombreMision + ". Recompensa: " + recompensa);

    return List.of(notificacion);
  }
}
