package grupo5.cliente.vistas;

import java.util.List;

/**
 * La bandeja de notificaciones del donante (RF-08), de la más nueva a la más vieja, y el "ahora"
 * con que el buzón dice hace cuánto llegó cada una.
 *
 * @param ahora fecha y hora local en ISO, a los minutos ("2026-10-15T16:25")
 */
public record DonanteNotificacionesVista(List<Aviso> avisos, String ahora) {

  /**
   * Una notificación como la lee el componente buzón: {@code fecha} en ISO a los minutos; {@code
   * tema}, el valor de su categoría (camino, entregadas, asignadas, deposito, necesidades,
   * problemas u otras); {@code destino}, a dónde lleva el mensaje.
   */
  public record Aviso(
      String id, String fecha, String texto, String tema, String destino, boolean leida) {}
}
