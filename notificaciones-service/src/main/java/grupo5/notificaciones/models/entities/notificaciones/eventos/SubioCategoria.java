package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class SubioCategoria extends EventoNotificable {
  private final String categoriaVieja;
  private final String categoriaNueva;

  public SubioCategoria(
      Persona persona, String categoriaVieja, String categoriaNueva, LocalDateTime fecha) {
    super(persona, fecha);
    this.categoriaVieja = categoriaVieja;
    this.categoriaNueva = categoriaNueva;
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    String mensaje =
        "¡Felicitaciones! Has ascendido de la categoría "
            + categoriaVieja
            + " a "
            + categoriaNueva
            + ".";
    Notificacion notificacion = new Notificacion(this.getPersona().getId(), mensaje);

    return List.of(notificacion);
  }
}
