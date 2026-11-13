package grupo5.cliente.vistas;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Donaciones recibidas (RF-11): lo asignado a la entidad, en el orden de lo que hay que hacer, con
 * las recibidas al final. {@code mes} es el mes en curso ("octubre"), {@code actualizado} es cuándo
 * se leyó ("15 de octubre de 2026, 18:30") y {@code novedades}, cuántas notificaciones hay sin
 * leer, o null si no se sabe.
 *
 * <p>Desde R11-43 los datos reales llegan de a una página ({@code asignadas} es la página {@code
 * pagina} de {@code paginas}, con {@code total} filas en todas): {@code cuantas} da cuántas hay de
 * cada grupo, contadas por Donaciones, y {@code enElMes}, cuántas se recibieron en el mes en curso.
 * La maqueta los deja en null y manda todo en una página: las cifras salen de la lista.
 */
public record RecibidasVista(
    List<Recibida> asignadas,
    String mes,
    String actualizado,
    Integer novedades,
    Map<String, Long> cuantas,
    Long enElMes,
    long total,
    int pagina,
    int paginas) {

  /**
   * Grupos de {@link Recibida#grupo()}. No van en el puerto: sus adaptadores ya usan esos nombres
   * para los estados de Donaciones.
   */
  public static final String PUERTA = "puerta";

  public static final String PROBLEMA = "problema";
  public static final String CAMINO = "camino";
  public static final String SALIR = "salir";
  public static final String RECIBIDA = "recibida";

  /** Lo que todavía no se recibió: lo que muestra Seguir entregas. */
  public static final Set<String> SIN_RECIBIR = Set.of(PUERTA, PROBLEMA, CAMINO, SALIR);

  private static final DateTimeFormatter MES =
      DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("es-AR"));

  /** Todo en una página, con las cifras de la lista (la maqueta y los tests de pantalla). */
  public RecibidasVista(
      List<Recibida> asignadas, String mes, String actualizado, Integer novedades) {
    this(asignadas, mes, actualizado, novedades, null, null, asignadas.size(), 1, 1);
  }

  /** La página que se muestra, para el paginador. */
  public Tramo<Recibida> tramo() {
    return new Tramo<>(asignadas, total, pagina, paginas);
  }

  /**
   * El mes de una recibida, por su fecha ("noviembre"): ya no son solo las del mes en curso. Sin
   * fecha, el mes en curso.
   */
  public String mesDe(Recibida recibida) {
    if (recibida.fecha() == null) {
      return mes;
    }
    try {
      return LocalDate.parse(recibida.fecha()).format(MES);
    } catch (DateTimeParseException e) {
      return mes;
    }
  }

  /**
   * Una donación asignada. {@code grupo} es {@code puerta}, {@code problema}, {@code camino},
   * {@code salir} o {@code recibida}; {@code categoria} es el ícono del sprite, o null; {@code
   * pedido} y {@code pedidoNombre}, la necesidad que cubre, o null (HB-17). {@code dia} y {@code
   * fecha} (ISO), solo en las recibidas. {@code llego}, cuándo y de dónde llegó, solo en las que
   * esperan en la puerta; {@code breve}, el paso en pocas palabras ("llega mañana"), en las que
   * todavía no llegaron. Los usan Inicio y Seguir entregas.
   */
  public record Recibida(
      String clave,
      String nombre,
      String categoria,
      String categoriaNombre,
      String cuanto,
      String dono,
      String pedido,
      String pedidoNombre,
      String grupo,
      String paso,
      String dia,
      String fecha,
      String llego,
      String breve) {}
}
