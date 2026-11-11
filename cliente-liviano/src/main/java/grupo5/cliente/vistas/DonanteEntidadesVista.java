package grupo5.cliente.vistas;

import java.util.List;

/**
 * Entidades del donante (RF-06): una página de entidades activas con lo que piden.
 *
 * @param total las entidades activas (con la búsqueda, si hay)
 * @param paginas cuántas páginas hay
 * @param dibujadas hasta qué página hay datos (la maqueta dibuja solo la primera)
 * @param actualizado cuándo se leyó ("hoy, 15 de octubre, 16:25")
 */
public record DonanteEntidadesVista(
    List<Entidad> entidades, long total, int paginas, int dibujadas, String actualizado) {

  /**
   * Una entidad: {@code ya}, las claves de las donaciones del donante que fueron a ella (vacía si
   * todavía no le donó); {@code pide}, sus necesidades vigentes.
   */
  public record Entidad(String nombre, List<String> ya, List<Pedido> pide) {}

  /**
   * Una necesidad: {@code cat}, el ícono de su categoría ("cat-alimentos"); {@code recibido}, null
   * mientras la API no lo da (HB-18): sin él, el renglón va sin barra.
   */
  public record Pedido(
      String que, String cat, String falta, String cuando, Integer recibido, Integer total) {}
}
