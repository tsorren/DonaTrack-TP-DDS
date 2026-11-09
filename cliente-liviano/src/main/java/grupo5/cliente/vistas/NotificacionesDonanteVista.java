package grupo5.cliente.vistas;

import java.util.List;

/**
 * Notificaciones del donante (RF-08): las noticias de sus donaciones, separadas en sin leer y
 * leídas, de la más nueva a la más vieja. {@code estado}, {@code claveDonacion} y {@code donacion}
 * vienen en la maqueta; la API de Notificaciones hoy solo manda el mensaje y la fecha, así que
 * quedan en null.
 */
public record NotificacionesDonanteVista(List<Noticia> sinLeer, List<Noticia> leidas) {

  public int cantidadSinLeer() {
    return sinLeer.size();
  }

  public boolean vacia() {
    return sinLeer.isEmpty() && leidas.isEmpty();
  }

  /**
   * Una noticia de la bandeja del donante. {@code cuando} es el texto legible ("Hoy, 14:30" o "13
   * de octubre, 11:00") y {@code fecha}, el ISO para {@code <time datetime>}.
   */
  public record Noticia(
      String texto,
      String cuando,
      String fecha,
      EstadoEntrega estado,
      String claveDonacion,
      String donacion) {}
}
