package grupo5.cliente.vistas;

import java.util.List;

/**
 * Mis donaciones (RF-05): una página de las donaciones del donante, de la más reciente a la
 * primera, las categorías del filtro y cuándo se leyeron ("hoy, 15 de octubre, 16:25"). {@code
 * total} cuenta todas las que cumplen los filtros, no solo las de la página; {@code pagina} va de 1
 * a {@code paginas} (al menos 1, aunque no haya ninguna).
 */
public record MisDonacionesVista(
    List<Donacion> donaciones,
    List<Categoria> categorias,
    String actualizado,
    long total,
    int pagina,
    int paginas) {

  /** Una sola página con todas las de la lista: el total es su tamaño. */
  public MisDonacionesVista(
      List<Donacion> donaciones, List<Categoria> categorias, String actualizado) {
    this(donaciones, categorias, actualizado, donaciones.size(), 1, 1);
  }

  /**
   * Una donación como la muestra el libro. {@code categoria} es el valor del filtro; {@code icono},
   * el del sprite de categorías, o null si no tiene; {@code destino}, null si todavía no tiene.
   */
  public record Donacion(
      String clave,
      long numero,
      String titulo,
      String cantidad,
      String categoria,
      String icono,
      String categoriaTexto,
      String destino,
      String donada,
      EstadoEntrega estado,
      String nota) {}

  /**
   * Una opción del filtro de categoría, con sus subcategorías para el filtro "Subcategoría" (RF-05,
   * R11-13; vacía si no tiene o no se saben).
   */
  public record Categoria(String valor, String texto, List<Categoria> subcategorias) {

    public Categoria {
      subcategorias = subcategorias == null ? List.of() : List.copyOf(subcategorias);
    }

    /** Sin subcategorías. */
    public Categoria(String valor, String texto) {
      this(valor, texto, List.of());
    }
  }
}
