package grupo5.cliente.vistas;

import java.util.List;

/**
 * Una página de la bandeja de notificaciones del donante (RF-08, R11-14), en el orden pedido, y el
 * "ahora" con que el buzón dice hace cuánto llegó cada una.
 *
 * @param pagina los avisos de la página, cuántos cumplen los filtros y la página que se muestra
 * @param sinLeer cuántos sin leer tiene la cuenta, también los de otras páginas (el conteo del
 *     servicio)
 * @param temas las categorías del filtro, todas las del rol; la primera es "Todas las categorías",
 *     con valor vacío
 * @param ahora fecha y hora local en ISO, a los minutos ("2026-10-15T16:25")
 */
public record DonanteNotificacionesVista(
    Tramo<Aviso> pagina, long sinLeer, List<Tema> temas, String ahora) {

  public DonanteNotificacionesVista {
    temas = List.copyOf(temas);
  }

  /**
   * Una notificación como la lee el componente buzón: {@code fecha} en ISO a los minutos; {@code
   * titulo}, qué pasó en pocas palabras (según el tipo de aviso); {@code texto}, el mensaje entero;
   * {@code tema}, el valor de su categoría; {@code destino}, a dónde lleva el vínculo, y {@code
   * vinculo}, su texto ("Ver la donación").
   */
  public record Aviso(
      String id,
      String fecha,
      String titulo,
      String texto,
      String tema,
      String destino,
      String vinculo,
      boolean leida) {

    /** El donante no tiene acción principal en la fila: la fila del buzón la pide igual. */
    public String boton() {
      return null;
    }

    /** Lo que nombra la acción principal, para lectores; el donante no tiene. */
    public String que() {
      return null;
    }

    /** El renglón chico debajo del mensaje; el donante no tiene. */
    public String nota() {
      return null;
    }
  }

  /** Una categoría del filtro: {@code icono}, el del buzón, o null en "Todas las categorías". */
  public record Tema(String valor, String texto, String icono) {}
}
