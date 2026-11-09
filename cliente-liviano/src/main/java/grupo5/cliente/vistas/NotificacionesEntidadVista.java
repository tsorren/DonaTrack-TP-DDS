package grupo5.cliente.vistas;

import java.util.List;

/**
 * Notificaciones de la entidad (RF-13): las novedades separadas en sin leer y leídas, de la más
 * reciente a la primera, y cuándo se consultaron ("15 de octubre de 2026, 18:30").
 */
public record NotificacionesEntidadVista(
    List<Novedad> sinLeer, List<Novedad> leidas, String actualizado, int marcadasRecien) {

  public boolean vacia() {
    return sinLeer.isEmpty() && leidas.isEmpty();
  }

  /**
   * Una fila del cuaderno de novedades. {@code tipo}, {@code tono}, {@code confirmo}, {@code
   * enlace}, {@code destino} y {@code que} vienen en la maqueta; la API de Notificaciones hoy solo
   * manda el mensaje y la fecha, así que quedan en null.
   */
  public record Novedad(
      String clave,
      String tipo,
      String tono,
      String dia,
      String hora,
      String fecha,
      String texto,
      String confirmo,
      String enlace,
      String destino,
      String que,
      boolean principal,
      boolean leida) {}
}
