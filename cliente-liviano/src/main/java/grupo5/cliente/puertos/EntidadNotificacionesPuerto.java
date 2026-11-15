package grupo5.cliente.puertos;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import java.util.List;
import java.util.UUID;

/** Las notificaciones de la entidad (RF-13): leerlas y marcarlas. {@code entidad} es su persona. */
public interface EntidadNotificacionesPuerto {

  /** Una página de la bandeja, con los filtros resueltos por la fuente (R11-14). */
  EntidadNotificacionesVista bandeja(UUID entidad, ConsultaBandeja consulta);

  /**
   * Si la fuente filtra la bandeja por la fecha del aviso ({@link ConsultaBandeja#desde()} y {@link
   * ConsultaBandeja#hasta()}, R11-B04). Notificaciones no la filtra: con ella, el campo no se
   * ofrece.
   */
  default boolean filtraPorFecha() {
    return true;
  }

  /** Marca esas como leídas; sin ninguna, todas las de la entidad. */
  void marcarLeidas(UUID entidad, List<UUID> ids);

  void dejarSinLeer(UUID id);
}
