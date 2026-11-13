package grupo5.cliente.vistas;

import java.util.List;

/**
 * Entidades del admin (RF-21 y RF-15): una sola lista, por nombre, con las que piden sumarse, las
 * activas y las inactivas. {@code activas} cuenta todas las activas (un número del servicio, no el
 * largo de la lista) y {@code al} dice cuándo se leyó ("15 de octubre de 2026, 18:30"). {@code
 * enPaginas}: si la lista es una página pedida al servicio (R11-01), con la búsqueda resuelta allá;
 * null si {@code todas} es la lista entera (la maqueta) y la pantalla filtra sobre ella.
 */
public record AdminEntidadesVista(
    long activas, String al, List<Entidad> todas, EnPaginas enPaginas) {

  /** La lista entera, sin páginas: la pantalla busca y filtra sobre ella. */
  public AdminEntidadesVista(long activas, String al, List<Entidad> todas) {
    this(activas, al, todas, null);
  }

  /**
   * Una página de las registradas (R11-01). {@code buscar}, el nombre que se buscó en el servidor,
   * sin distinguir mayúsculas ni tildes ('' si no); {@code total}, cuántas cumplen la búsqueda y
   * los filtros contando las que piden sumarse; {@code registradas}, el tramo que pidió el
   * servicio, para el paginador. Las que piden sumarse van primero y solo en la página 1. Desde
   * R11-43, {@code inactivas} y {@code pendientes} cuentan todas las de baja y todas las que piden
   * sumarse, como {@code activas}: no dependen de la búsqueda ni de los filtros.
   */
  public record EnPaginas(
      String buscar, long total, Tramo<Entidad> registradas, long inactivas, long pendientes) {}

  /**
   * Los filtros de la lista (R11-43): {@code ver} es {@code activas}, {@code pendientes}, {@code
   * inactivas} o '' (todas); {@code pedidos}, {@code con}, {@code sin} o '' (necesidades vigentes);
   * {@code tipo}, el rubro ('' si no). Un valor que no es uno de esos queda en ''.
   */
  public record Filtro(String ver, String pedidos, String tipo) {

    private static final List<String> VER = List.of("activas", "pendientes", "inactivas");
    private static final List<String> PEDIDOS = List.of("con", "sin");

    /** Los rubros que ofrece el menú Tipo. */
    public static final List<String> TIPOS =
        List.of(
            "Comedor", "Merendero", "Escuela", "Hogar", "Club", "Biblioteca", "Centro comunitario");

    /** Sin filtros. Va después de las listas: el constructor las usa. */
    public static final Filtro NINGUNO = new Filtro("", "", "");

    public Filtro {
      ver = ver != null && VER.contains(ver) ? ver : "";
      pedidos = pedidos != null && PEDIDOS.contains(pedidos) ? pedidos : "";
      tipo = tipo != null && TIPOS.contains(tipo) ? tipo : "";
    }
  }

  /**
   * Una entidad. {@code clave} la elige en la dirección y viaja al aprobarla; {@code estado} es
   * {@code activa}, {@code pendiente} o {@code inactiva}; {@code cat}, el ícono del sprite. Lo que
   * no se sabe queda null y la pantalla no lo muestra: de una activa, {@code alta} y las entregas
   * confirmadas en el mes ({@code entregas}) y desde la activación ({@code total}); de una
   * pendiente, cuándo pidió sumarse ({@code cuando}, {@code espera}), {@code falta} (lo que le
   * falta mandar), sus datos ({@code razon}, {@code cuit}, {@code direccion}) y {@code constancia};
   * de una inactiva, {@code baja}, {@code ultima} (su última actividad) y {@code motivo}.
   */
  public record Entidad(
      String clave,
      String estado,
      String nombre,
      String tipo,
      String cat,
      String localidad,
      String responsable,
      String correo,
      String mision,
      List<Necesidad> necesidades,
      String alta,
      Integer entregas,
      Integer total,
      String cuando,
      String espera,
      String falta,
      String razon,
      String cuit,
      String direccion,
      String constancia,
      String baja,
      String ultima,
      String motivo) {}

  /** Una necesidad vigente: qué pide, el ícono de su categoría y lo que falta o pide. */
  public record Necesidad(String que, String cat, String falta) {}
}
