package grupo5.cliente.puertos;

import grupo5.cliente.vistas.AdminNotificacionesVista;
import grupo5.cliente.vistas.ConsultaBandeja;
import java.util.List;
import java.util.UUID;

/**
 * La bandeja de la Administración (R11-30): leerla de a una página y marcarla. Es una sola para
 * todas las cuentas de administración, así que no lleva persona.
 */
public interface AdminNotificacionesPuerto {

  /** Una página de la bandeja, con los filtros resueltos por la fuente (R11-14). */
  AdminNotificacionesVista bandeja(ConsultaBandeja consulta);

  /** Marca esas como leídas; sin ninguna, todas las de la Administración. */
  void marcarLeidas(List<UUID> ids);

  void dejarSinLeer(UUID id);
}
