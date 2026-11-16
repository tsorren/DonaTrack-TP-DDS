package grupo5.notificaciones.seguridad;

import grupo5.common.seguridad.ReglasDeAcceso;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Rutas de Notificaciones que no piden token: el formulario de contacto (HB-21). La regla es por
 * ruta, sin método: el listado de {@code GET /api/contacto} sigue siendo del ADMIN por su
 * {@code @PreAuthorize}.
 */
@Component
public class ReglasDeAccesoNotificaciones implements ReglasDeAcceso {

  @Override
  public List<String> rutasPublicas() {
    return List.of("/api/contacto");
  }
}
