package grupo5.notificaciones.seguridad;

import grupo5.notificaciones.services.impl.NotificacionService;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Dueño de cada recurso, para {@code principal.puedeOperarSobre(@duenioDe.notificacion(#id))}. Una
 * notificación inexistente no tiene dueño ({@code null}): quien no es admin recibe 403, igual que
 * con una ajena, y no puede averiguar qué ids existen.
 */
@Component("duenioDe")
public class DuenioDe {

  private final NotificacionService notificaciones;

  public DuenioDe(NotificacionService notificaciones) {
    this.notificaciones = notificaciones;
  }

  public UUID notificacion(UUID id) {
    return notificaciones.personaDe(id).orElse(null);
  }
}
