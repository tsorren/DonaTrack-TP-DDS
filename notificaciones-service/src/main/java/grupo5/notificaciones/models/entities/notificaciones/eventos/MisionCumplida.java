package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class MisionCumplida extends EventoNotificable {
  private final String nombreMision;
  private final String recompensa;

  public MisionCumplida(
      Persona persona, String nombreMision, String recompensa, LocalDateTime fecha) {
    super(persona, fecha);
    this.nombreMision = nombreMision;
    this.recompensa = recompensa;
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(
            this.getPersona().getId(),
            "Completaste la misión " + nombreMision + ". Recompensa: " + recompensa);

    return List.of(notificacion);
  }
}
