package grupo5.cliente.puertos;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import java.util.List;
import java.util.UUID;

/** Las notificaciones de la entidad (RF-13): leerlas y marcarlas. {@code entidad} es su persona. */
public interface EntidadNotificacionesPuerto {

  /** Una página de la bandeja, con los filtros resueltos por la fuente (R11-14). */
  EntidadNotificacionesVista bandeja(UUID entidad, ConsultaBandeja consulta);

  /** Marca esas como leídas; sin ninguna, todas las de la entidad. */
  void marcarLeidas(UUID entidad, List<UUID> ids);

  void dejarSinLeer(UUID id);
}
