package grupo5.cliente.puertos;

import grupo5.cliente.vistas.NotificacionesDonanteVista;
import java.util.UUID;

/** De dónde salen las notificaciones del donante (RF-08): de Notificaciones o de la maqueta. */
public interface NotificacionesDonantePuerto {

  NotificacionesDonanteVista notificaciones(UUID donante);

  void marcarTodasLeidas(UUID donante);
}
