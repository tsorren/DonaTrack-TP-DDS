package grupo5.cliente.vistas;

import java.util.List;

/**
 * Mis donaciones (RF-05): las donaciones del donante, de la más reciente a la primera, las
 * categorías del filtro y cuándo se leyeron ("hoy, 15 de octubre, 16:25").
 */
public record MisDonacionesVista(
    List<Donacion> donaciones, List<Categoria> categorias, String actualizado) {

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

  /** Una opción del filtro de categoría. */
  public record Categoria(String valor, String texto) {}
}
