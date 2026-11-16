package grupo5.cliente.puertos;

import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.RecibidasVista;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Las donaciones asignadas a una entidad, para Donaciones recibidas (RF-11). */
public interface RecibidasPuerto {

  /**
   * Qué página de Donaciones recibidas se pide (R11-B04, componente listado): los {@code grupos}
   * (vacío: todos), la página desde 1, {@code desde} y {@code hasta} (días inclusivos sobre la
   * fecha de registro; null no acota), {@code categoria} (el valor de una de {@link #categorias()};
   * null no filtra), {@code donante} (parte del nombre de quien donó, sin mayúsculas ni tildes;
   * null no filtra) y {@code orden}: '' (lo que hay que atender primero), {@link #RECIENTES} o
   * {@link Listado#ANTIGUAS}, por la fecha de registro dentro de cada grupo.
   */
  record Consulta(
      Set<String> grupos,
      int pagina,
      LocalDate desde,
      LocalDate hasta,
      String categoria,
      String donante,
      String orden) {

    public static final String RECIENTES = "recientes";

    public Consulta {
      grupos = Set.copyOf(grupos);
      pagina = Math.max(1, pagina);
      categoria = categoria == null || categoria.isBlank() ? null : categoria.strip();
      donante = donante == null || donante.isBlank() ? null : donante.strip();
      orden = RECIENTES.equals(orden) || Listado.ANTIGUAS.equals(orden) ? orden : "";
    }

    /** Solo los grupos y la página. */
    public static Consulta de(Set<String> grupos, int pagina) {
      return new Consulta(grupos, pagina, null, null, null, null, "");
    }
  }

  /**
   * Para Inicio: lo asignado, con las recibidas del mes en curso. Los datos reales leen una sola
   * página de las más urgentes primero (R11-43).
   */
  RecibidasVista recibidas(UUID entidad);

  /**
   * Una página de lo asignado (R11-43), solo de los {@code grupos} pedidos (vacío: todos), con las
   * cifras de cada grupo; {@code pagina} desde 1.
   */
  default RecibidasVista recibidas(UUID entidad, Set<String> grupos, int pagina) {
    return recibidas(entidad, Consulta.de(grupos, pagina));
  }

  /**
   * Una página con los filtros de {@code consulta}. Por defecto, lo mismo que {@link
   * #recibidas(UUID)}: la plantilla agrupa y filtra por estado.
   */
  default RecibidasVista recibidas(UUID entidad, Consulta consulta) {
    return recibidas(entidad);
  }

  /** Las categorías del filtro (valor y texto), sin la de "todas". */
  default List<Listado.Opcion> categorias() {
    return List.of();
  }

  /**
   * Si la fuente filtra por el nombre de quien donó ({@link Consulta#donante()}). Donaciones solo
   * filtra por el id del donante: con ella, el campo no se ofrece.
   */
  default boolean filtraPorDonante() {
    return true;
  }
}
