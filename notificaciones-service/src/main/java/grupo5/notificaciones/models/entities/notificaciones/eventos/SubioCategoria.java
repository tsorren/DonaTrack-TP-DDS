package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.persona.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubioCategoria extends EventoNotificable {
  private String categoriaVieja;
  private String categoriaNueva;

  public SubioCategoria(
      Persona persona, String categoriaVieja, String categoriaNueva, LocalDateTime fecha) {
    super();
    this.categoriaVieja = categoriaVieja;
    this.categoriaNueva = categoriaNueva;
  }

  public SubioCategoria() {}

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacion =
        new Notificacion(
            this.getPersona(),
            "Subiste a la categoría " + categoriaNueva + "desde la categoria" + categoriaVieja);

    return List.of(notificacion);
  }
}
