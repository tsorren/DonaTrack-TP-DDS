package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteEntidadesVista;
import java.util.UUID;

/**
 * Las entidades que ve el donante, de a seis por página. Lo implementan la maqueta y el adaptador
 * REST; {@code cliente.datos.donante} elige cuál.
 */
public interface DonanteEntidadesPuerto {

  /**
   * Qué página se pide y con qué filtros (R11-12): {@code nombre}, vacío para todas; {@code
   * categoria}, el valor de una de las categorías de la vista (una que no está no filtra); {@code
   * soloDonadas}, solo a las que el donante ya les donó; {@code pagina}, desde 1. {@code alReves}
   * (R11-B04): de la Z a la A; sin él, de la A a la Z.
   */
  record Consulta(
      String nombre, String categoria, boolean soloDonadas, int pagina, boolean alReves) {

    public Consulta {
      nombre = nombre == null ? "" : nombre.strip();
      categoria = categoria == null || categoria.isBlank() ? null : categoria.strip();
      pagina = Math.max(1, pagina);
    }

    /** De la A a la Z. */
    public Consulta(String nombre, String categoria, boolean soloDonadas, int pagina) {
      this(nombre, categoria, soloDonadas, pagina, false);
    }
  }

  /**
   * Si la fuente ordena de la Z a la A (R11-B04). Por defecto sí (la maqueta); Donaciones solo
   * ordena por nombre de la A a la Z, así que el adaptador REST no ofrece el orden.
   */
  default boolean ordenaAlReves() {
    return true;
  }

  /**
   * Una página con la búsqueda y los filtros resueltos sobre todas las entidades activas. Una
   * página más allá de la última muestra la última.
   */
  DonanteEntidadesVista entidades(UUID donante, Consulta consulta);
}
