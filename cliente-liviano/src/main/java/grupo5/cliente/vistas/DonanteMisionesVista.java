package grupo5.cliente.vistas;

import java.util.List;

/**
 * Misiones e insignias de la persona que dona (RF-07): las seis misiones en orden, las tres
 * categorías y cómo va cada una. Lo que la API no da (la historia de cada insignia, el "tu donación
 * más grande") viaja como null y la pantalla no lo muestra.
 *
 * @param persona si la cuenta es una persona (las empresas no tienen misiones); null en la maqueta,
 *     donde la cuenta de ejemplo la elige {@code ?cuenta=}
 * @param nueva si todavía no completó ninguna misión
 * @param nombre cómo se lo saluda ("Martina")
 * @param categoria su categoría ("Sostenedor")
 * @param desde desde cuándo es de esa categoría ("2 de septiembre de 2026"), o null si no se sabe
 * @param completadas cuántas misiones completó
 * @param actualizado cuándo se leyó ("15 de octubre de 2026, 16:25")
 * @param bienvenida la historia de la primera planta, la que llega con la cuenta
 * @param nota lo que la pantalla avisa que todavía no muestra, o null si lo muestra todo
 */
public record DonanteMisionesVista(
    Boolean persona,
    boolean nueva,
    String nombre,
    String categoria,
    String desde,
    int completadas,
    String actualizado,
    List<Mision> misiones,
    List<Categoria> categorias,
    Historia bienvenida,
    String nota) {

  /**
   * Una misión. {@code estado}: {@code completa}, {@code curso} (la única que Incentivos evalúa),
   * {@code espera} (la que sigue a la que está en curso, en su categoría) o {@code cerrada} (de una
   * categoría que todavía no se abre); {@code fecha}: cuándo la completó; {@code progreso}, {@code
   * objetivo} y {@code cuenta}: cómo va la que está en curso ("12 de 50"); {@code espera}: la que
   * tiene que completar antes; {@code nota}: un dato extra, o null; {@code historia}: qué hizo para
   * completarla, o null si no se sabe.
   */
  public record Mision(
      String categoria,
      String nombre,
      String pide,
      String estado,
      String fecha,
      Integer progreso,
      Integer objetivo,
      String cuenta,
      String nota,
      String espera,
      Historia historia) {}

  /**
   * Una categoría. {@code estado}: {@code completa}, {@code tu categoría} o {@code se abre
   * después}; {@code siguiente}: la que viene después de la actual; {@code anterior}: la que hay
   * que completar para abrirla.
   */
  public record Categoria(String nombre, String estado, String siguiente, String anterior) {}

  /**
   * Qué hizo para ganar una planta de la huerta. {@code categoria}: el ícono de lo donado ({@code
   * cat-alimentos}), o null; {@code donacion}: la clave de la donación (sin ella no hay "Ver la
   * donación"); {@code entidad} y {@code mensaje}: lo que le escribió la entidad.
   */
  public record Historia(
      String hizo,
      List<Hito> hitos,
      String donacion,
      String entidad,
      String mensaje,
      String categoria) {}

  /** Un paso de la historia: qué pasó, cuándo y si ya pasó. */
  public record Hito(String texto, String fecha, boolean hecho) {}
}
