package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteTableroVista;
import java.util.UUID;

/**
 * El inicio del donante. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.donante} elige cuál.
 */
public interface DonanteInicioPuerto {

  /** El inicio de la empresa. */
  DonanteInicioVista inicio(UUID donante);

  /**
   * El tablero de la empresa (R11-25): la página pedida de cada columna (desde 1; la que se pasa,
   * la última que hay), de a {@link DonanteTableroVista#POR_COLUMNA}, con su total, y la cifra de
   * las que esperan destino. Nunca la lista entera.
   */
  DonanteTableroVista tablero(UUID donante, int tener, int camino, int llego);

  /** El inicio de la persona: su año mes a mes, con las cifras como números. */
  DonanteAnioVista anio(UUID donante);
}
