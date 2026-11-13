package grupo5.cliente.vistas;

import java.util.List;

/**
 * Una página de la bandeja de la Administración (R11-30, spec R11-p2-campanita §3.3), para el
 * componente buzón ({@code componentes/buzon :: pagina}). Es la bandeja compartida de todas las
 * cuentas de administración. {@code sinLeer}, cuántas sin leer tiene, también las de otras páginas
 * (el conteo del servicio). {@code ahora} es el momento de la lectura en ISO ("2026-10-15T18:45"),
 * del que salen los "hace cuánto", y {@code actualizado}, el mismo en palabras ("15 de octubre de
 * 2026, 18:45"). {@code temas}, las categorías del filtro, todas las del rol: la primera es "Todas
 * las categorías", con valor vacío.
 */
public record AdminNotificacionesVista(
    Tramo<Aviso> pagina, long sinLeer, List<Tema> temas, String ahora, String actualizado) {

  public AdminNotificacionesVista {
    temas = List.copyOf(temas);
  }

  /**
   * Una notificación. {@code fecha} en ISO; {@code titulo}, qué pasó en pocas palabras; {@code
   * texto}, el mensaje entero; {@code destino}, a dónde lleva, o null si no tiene adónde (el aviso
   * de contacto) y {@code vinculo}, el texto del vínculo. {@code boton}, {@code que} y {@code nota}
   * son los del buzón y en la administración van siempre en null.
   */
  public record Aviso(
      String id,
      String tema,
      String fecha,
      boolean leida,
      String titulo,
      String texto,
      String vinculo,
      String destino) {

    public String boton() {
      return null;
    }

    public String que() {
      return null;
    }

    public String nota() {
      return null;
    }
  }

  /** Una categoría del filtro: {@code icono}, el del buzón, o null en "Todas las categorías". */
  public record Tema(String valor, String texto, String icono) {}
}
