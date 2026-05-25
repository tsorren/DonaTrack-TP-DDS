package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubioCategoria extends EventoNotificable {
  private String categoria;

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(this.getPersona(), "Subiste a la categoría " + categoria);

    return List.of(notificacion);
  }
}
