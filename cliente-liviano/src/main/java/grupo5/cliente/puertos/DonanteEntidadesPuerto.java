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
   * soloDonadas}, solo a las que el donante ya les donó; {@code pagina}, desde 1.
   */
  record Consulta(String nombre, String categoria, boolean soloDonadas, int pagina) {

    public Consulta {
      nombre = nombre == null ? "" : nombre.strip();
      categoria = categoria == null || categoria.isBlank() ? null : categoria.strip();
      pagina = Math.max(1, pagina);
    }
  }

  /**
   * Una página con la búsqueda y los filtros resueltos sobre todas las entidades activas. Una
   * página más allá de la última muestra la última.
   */
  DonanteEntidadesVista entidades(UUID donante, Consulta consulta);
}
