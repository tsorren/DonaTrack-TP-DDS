package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import java.util.UUID;

/**
 * El inicio del donante. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.donante} elige cuál.
 */
public interface DonanteInicioPuerto {

  /** El inicio de la empresa. */
  DonanteInicioVista inicio(UUID donante);

  /** El inicio de la persona: su año mes a mes, con las cifras como números. */
  DonanteAnioVista anio(UUID donante);
}
