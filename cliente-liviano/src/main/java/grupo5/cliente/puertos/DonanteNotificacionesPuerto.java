package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteNotificacionesVista;
import java.util.List;
import java.util.UUID;

/**
 * Las notificaciones del donante. {@code persona} es la de la cuenta (en Donaciones, el id del
 * donante es el de su persona). Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.donante} elige cuál.
 */
public interface DonanteNotificacionesPuerto {

  DonanteNotificacionesVista bandeja(UUID persona);

  /**
   * Marca como leídas (o, con {@code leida} false, como no leídas) las de esos ids; solo las que
   * están en la bandeja de la persona.
   */
  void marcar(UUID persona, List<String> ids, boolean leida);

  void marcarTodas(UUID persona);
}
