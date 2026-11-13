package grupo5.cliente.puertos;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.DonanteNotificacionesVista;
import java.util.List;
import java.util.UUID;

/**
 * Las notificaciones del donante. {@code persona} es la de la cuenta (en Donaciones, el id del
 * donante es el de su persona). Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.donante} elige cuál.
 */
public interface DonanteNotificacionesPuerto {

  /** Una página de la bandeja, con los filtros resueltos por la fuente (R11-14). */
  DonanteNotificacionesVista bandeja(UUID persona, ConsultaBandeja consulta);

  /**
   * Marca como leídas (o, con {@code leida} false, como no leídas) las de esos ids. Quién es dueño
   * de cada una lo controla Notificaciones: un id ajeno o que no existe se saltea.
   */
  void marcar(UUID persona, List<String> ids, boolean leida);

  void marcarTodas(UUID persona);
}
