package ar.edu.utn.frba.ddsi.notificaciones.models.entities.notificaciones;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonanteRegistrado extends EventoNotificable {
  private String bienvenida;
  private String credencialesDeAcceso;

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(this.getPersona(), bienvenida + "\n" + credencialesDeAcceso);

    return List.of(notificacion);
  }
}
