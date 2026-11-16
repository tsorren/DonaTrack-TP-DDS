package grupo5.notificaciones.models.repositories;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;

/**
 * Una página de la bandeja y cuántas cumplen los filtros, sin contar la página. Las notificaciones
 * no traen su historial de estados: la bandeja no lo muestra.
 */
public record PaginaBandeja(List<Notificacion> notificaciones, long total) {

  public PaginaBandeja {
    notificaciones = List.copyOf(notificaciones);
  }
}
