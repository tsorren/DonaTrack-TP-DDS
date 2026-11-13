package grupo5.cliente.vistas;

import java.util.List;

/**
 * El Ranking de donantes del admin (RF-19): los meses ya calculados, del más reciente al más viejo,
 * y el nombre del mes en curso, que todavía no tiene ranking.
 *
 * <p>Desde R11-43 los datos reales llegan de a una página de meses ({@code meses} es la página
 * {@code pagina} de {@code paginas}, con {@code total} meses en todas) y {@code elegido} es el mes
 * que se muestra, aunque esté en otra página, o null si la plantilla elige (el pedido en la
 * dirección o el primero).
 */
public record RankingVista(
    List<Mes> meses, String proximo, Mes elegido, long total, int pagina, int paginas) {

  /** Los meses que se muestran en el menú de una página (un año). */
  public static final int POR_PAGINA = 12;

  public RankingVista {
    meses = List.copyOf(meses);
  }

  /** Todos los meses en una página, sin elegido (la maqueta y los tests de pantalla). */
  public RankingVista(List<Mes> meses, String proximo) {
    this(meses, proximo, null, meses.size(), 1, 1);
  }

  /**
   * Un mes del ranking. {@code clave} es {@code 2026-09}; {@code calculado} es la fecha en que se
   * calculó, ya escrita para la pantalla, o {@code null} si el ranking no la guardó.
   */
  public record Mes(String clave, String nombre, String calculado, List<Puesto> puestos) {

    public Mes {
      puestos = List.copyOf(puestos);
    }
  }

  /** Un donante en el ranking, en el orden de su puesto. */
  public record Puesto(String donante, long misiones) {}
}
