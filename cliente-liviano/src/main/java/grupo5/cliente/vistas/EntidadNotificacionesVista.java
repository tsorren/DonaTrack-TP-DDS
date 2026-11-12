package grupo5.cliente.vistas;

import java.util.List;

/**
 * La bandeja de Notificaciones de la entidad (RF-13), para el componente buzón. {@code ahora} es el
 * momento de la lectura en ISO ("2026-10-15T18:30"), del que salen los "hace cuánto", y {@code
 * actualizado}, el mismo en palabras ("15 de octubre de 2026, 18:30"). {@code temas}, las
 * categorías del filtro: la primera es "Todas las categorías", con valor vacío.
 */
public record EntidadNotificacionesVista(
    List<Novedad> novedades, List<Tema> temas, String ahora, String actualizado) {

  /**
   * Una notificación. {@code fecha} en ISO; {@code destino}, a dónde lleva; {@code boton} y {@code
   * que}, la acción principal (la que espera en la puerta), o null; {@code nota}, un renglón chico,
   * o null.
   */
  public record Novedad(
      String id,
      String tema,
      String fecha,
      boolean leida,
      String texto,
      String boton,
      String que,
      String destino,
      String nota) {}

  /** Una categoría del filtro: {@code icono}, el del buzón, o null en "Todas las categorías". */
  public record Tema(String valor, String texto, String icono) {}
}
