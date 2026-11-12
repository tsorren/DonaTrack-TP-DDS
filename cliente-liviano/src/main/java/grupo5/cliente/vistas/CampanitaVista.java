package grupo5.cliente.vistas;

import java.util.List;

/**
 * El desplegable de la campanita (R11-29, ficha docs/interfaz/flujos/r11-p2-campanita.md): hasta 5
 * notificaciones sin leer, las más nuevas primero, y cuántas sin leer tiene la cuenta.
 *
 * @param filas hasta 5, con su título y su fecha
 * @param sinLeer cuántas sin leer tiene la cuenta (el total del servicio, no el largo de la lista)
 * @param ahora fecha y hora local en ISO, a los minutos, con que se dice hace cuánto llegó cada una
 */
public record CampanitaVista(List<Fila> filas, long sinLeer, String ahora) {

  /** Cuántas muestra el desplegable como máximo. */
  public static final int MAXIMO = 5;

  public CampanitaVista {
    filas = List.copyOf(filas);
  }

  /** Una notificación: {@code fecha} en ISO a los minutos ("2026-10-15T16:25"). */
  public record Fila(String id, String titulo, String fecha) {}
}
