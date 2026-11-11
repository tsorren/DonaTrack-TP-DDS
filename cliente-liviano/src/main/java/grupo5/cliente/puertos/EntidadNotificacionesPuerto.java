package grupo5.cliente.puertos;

import grupo5.cliente.vistas.EntidadNotificacionesVista;
import java.util.List;
import java.util.UUID;

/** Las notificaciones de la entidad (RF-13): leerlas y marcarlas. {@code entidad} es su persona. */
public interface EntidadNotificacionesPuerto {

  EntidadNotificacionesVista bandeja(UUID entidad);

  /** Marca esas como leídas; sin ninguna, todas las de la entidad. */
  void marcarLeidas(UUID entidad, List<UUID> ids);

  void dejarSinLeer(UUID id);
}
