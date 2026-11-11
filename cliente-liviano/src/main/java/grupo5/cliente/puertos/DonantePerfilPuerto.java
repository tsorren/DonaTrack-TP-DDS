package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.UUID;

/**
 * Mi perfil del donante. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.donante} elige cuál.
 */
public interface DonantePerfilPuerto {

  DonantePerfilVista perfil(UUID donante);

  /** Si las entidades ven el nombre del donante en lo que reciben. */
  void mostrarNombre(UUID donante, boolean mostrar);
}
