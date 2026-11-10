package grupo5.cliente.vistas;

import java.util.List;

/**
 * El Ranking de donantes del admin (RF-19): los meses ya calculados, del más reciente al más viejo,
 * y el nombre del mes en curso, que todavía no tiene ranking.
 */
public record RankingVista(List<Mes> meses, String proximo) {

  public RankingVista {
    meses = List.copyOf(meses);
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
