package ar.edu.utn.frba.ddsi.notificaciones.models.entities.notificaciones;

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
