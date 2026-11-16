package grupo5.cliente.vistas;

import java.util.List;
import java.util.stream.Stream;

/**
 * Entidades del donante (RF-06): una página de entidades activas con lo que piden. La búsqueda, la
 * categoría y "Ya doné" ya vienen resueltas sobre todas (R11-12): la plantilla no vuelve a filtrar.
 *
 * @param total las entidades que cumplen la búsqueda y los filtros
 * @param pagina la página que se muestra, desde 1 (ya corregida si la pedida no existía)
 * @param paginas cuántas páginas hay
 * @param dibujadas hasta qué página hay datos (la maqueta dibuja solo la primera)
 * @param categorias las del filtro y de los grupos, en el orden en que se muestran
 * @param actualizado cuándo se leyó ("hoy, 15 de octubre, 16:25")
 */
public record DonanteEntidadesVista(
    List<Entidad> entidades,
    long total,
    int pagina,
    int paginas,
    int dibujadas,
    List<Categoria> categorias,
    String actualizado) {

  public DonanteEntidadesVista {
    entidades = List.copyOf(entidades);
    categorias = List.copyOf(categorias);
  }

  /** Las opciones del filtro "Categoría": primero "Todas las categorías" (valor vacío). */
  public List<Categoria> opcionesDeCategoria() {
    return Stream.concat(
            Stream.of(new Categoria("", "Todas las categorías", null)), categorias.stream())
        .toList();
  }

  /** La página como {@link Tramo}, para el paginador. */
  public Tramo<Entidad> tramo() {
    return new Tramo<>(entidades, total, pagina, paginas);
  }

  /** Una entidad: {@code ya}, si el donante ya le donó; {@code pide}, sus necesidades vigentes. */
  public record Entidad(String nombre, boolean ya, List<Pedido> pide) {}

  /**
   * Una necesidad: {@code categoria}, el valor de su categoría (null si no se sabe: va en "Otros
   * pedidos"); {@code cat}, el ícono de esa categoría ("cat-alimentos"); {@code recibido}, null
   * mientras la API no lo da (HB-18): sin él, el renglón va sin barra.
   */
  public record Pedido(
      String que,
      String categoria,
      String cat,
      String falta,
      String cuando,
      Integer recibido,
      Integer total) {}

  /** Una categoría del filtro y de los grupos: su valor en la dirección, su nombre y su ícono. */
  public record Categoria(String valor, String texto, String icono) {}
}
