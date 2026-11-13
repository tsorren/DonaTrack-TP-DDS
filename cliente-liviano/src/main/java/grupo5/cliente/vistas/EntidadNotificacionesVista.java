package grupo5.cliente.vistas;

import java.util.List;

/**
 * Una página de la bandeja de Notificaciones de la entidad (RF-13, R11-14), para el componente
 * buzón. {@code sinLeer}, cuántas sin leer tiene la cuenta, también las de otras páginas (el conteo
 * del servicio). {@code ahora} es el momento de la lectura en ISO ("2026-10-15T18:30"), del que
 * salen los "hace cuánto", y {@code actualizado}, el mismo en palabras ("15 de octubre de 2026,
 * 18:30"). {@code temas}, las categorías del filtro, todas las del rol: la primera es "Todas las
 * categorías", con valor vacío.
 */
public record EntidadNotificacionesVista(
    Tramo<Novedad> pagina, long sinLeer, List<Tema> temas, String ahora, String actualizado) {

  public EntidadNotificacionesVista {
    temas = List.copyOf(temas);
  }

  /**
   * Una notificación. {@code fecha} en ISO; {@code titulo}, qué pasó en pocas palabras; {@code
   * texto}, el mensaje entero; {@code destino}, a dónde lleva y {@code vinculo}, el texto del
   * vínculo; {@code boton} y {@code que}, la acción principal (la que espera en la puerta), o null,
   * que reemplaza al vínculo; {@code nota}, un renglón chico, o null.
   */
  public record Novedad(
      String id,
      String tema,
      String fecha,
      boolean leida,
      String titulo,
      String texto,
      String vinculo,
      String boton,
      String que,
      String destino,
      String nota) {}

  /** Una categoría del filtro: {@code icono}, el del buzón, o null en "Todas las categorías". */
  public record Tema(String valor, String texto, String icono) {}
}
