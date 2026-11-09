package grupo5.cliente.puertos;

import grupo5.cliente.vistas.NotificacionesEntidadVista;
import java.util.UUID;

/** De dónde salen las notificaciones de la entidad (RF-13): de Notificaciones o de la maqueta. */
public interface NotificacionesEntidadPuerto {

  NotificacionesEntidadVista novedades(UUID entidad);

  void marcarTodasLeidas(UUID entidad);
}
