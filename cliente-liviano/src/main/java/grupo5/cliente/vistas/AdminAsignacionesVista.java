package grupo5.cliente.vistas;

import java.util.List;
import java.util.stream.Stream;

/**
 * Asignar donaciones del admin (RF-17, R11-24; ficha r11-p1-asignaciones): una pestaña por vez.
 * {@code consulta} es lo que se pidió; {@code cuentas}, las cifras de las pestañas (del resumen de
 * Donaciones, no de la página); {@code ultima}, la última asignación ("hoy, 0:00 · generó 12
 * propuestas", o null si nunca corrió). Según la pestaña vienen {@code propuestas} (Propuestas,
 * Conflictos, la donación elegida en Destino y Todas las decididas, que trae {@code decididas}),
 * {@code recientes} (las 3 últimas decididas, en Propuestas), {@code donaciones} (Destino, paso 1)
 * y {@code elegida} (Destino, paso 2; null si la donación pedida ya no está en ninguna pendiente).
 * Lo que no corresponde a la pestaña es null o vacío.
 */
public record AdminAsignacionesVista(
    ConsultaAsignaciones consulta,
    Cuentas cuentas,
    String ultima,
    Tramo<Propuesta> propuestas,
    List<Decidida> recientes,
    Tramo<Decidida> decididas,
    List<Opcion> categorias,
    Tramo<DonacionEnJuego> donaciones,
    DonacionEnJuego elegida) {

  /** Los nombres de los algoritmos en pantalla (ficha, sección 6). */
  public static final String PARECIDO = "Parecido con lo pedido";

  public static final String MENOS_RECIBIERON = "Quienes menos recibieron";

  public AdminAsignacionesVista {
    recientes = recientes == null ? List.of() : List.copyOf(recientes);
    categorias = categorias == null ? List.of() : List.copyOf(categorias);
  }

  /** Las opciones del filtro "Ver" de Propuestas: la primera es "Todas". */
  public List<Opcion> algoritmos() {
    return List.of(
        new Opcion("", "Todas"),
        new Opcion(ConsultaAsignaciones.PARECIDO, PARECIDO),
        new Opcion(ConsultaAsignaciones.MENOS_RECIBIERON, MENOS_RECIBIERON),
        new Opcion(ConsultaAsignaciones.LAS_DOS, "Las dos"));
  }

  /** Las opciones del filtro de categoría de Destino: la primera es "Todas". */
  public List<Opcion> categoriasDelFiltro() {
    return Stream.concat(Stream.of(new Opcion("", "Todas")), categorias.stream()).toList();
  }

  /** El nombre del algoritmo elegido en el filtro, para el vacío ("… no sugirió ninguna"). */
  public String algoritmoElegido() {
    return algoritmos().stream()
        .filter(o -> o.valor().equals(consulta.algoritmo()))
        .map(Opcion::texto)
        .findFirst()
        .orElse("");
  }

  /**
   * Las cifras de las pestañas: {@code pendientes}, todas; {@code sinProblemas}, las que se pueden
   * aprobar de una (Propuestas); {@code enConflicto}, las que chocan o ya no alcanzan (Conflictos).
   */
  public record Cuentas(long pendientes, long sinProblemas, long enConflicto) {}

  /**
   * Una propuesta pendiente (la tarjeta común de las tres pestañas). {@code sugeridaPor}: una
   * pastilla por algoritmo; {@code nota}: la letra chica debajo ("Las donaciones son las que
   * eligió…", o que falta el dato), o null. {@code situacion}: {@code libre}, {@code conflicto} o
   * {@code sin-stock} (sin Aprobar). {@code franja}: el choque, en Conflictos (null si no choca);
   * {@code comparar}: la clave de la donación por la que compite, para "Comparar en Destino" (null
   * si no compite por una donación).
   */
  public record Propuesta(
      String clave,
      String entidad,
      String pide,
      String cuando,
      String cubre,
      String alcance,
      boolean nueva,
      List<Sugerencia> sugeridaPor,
      String nota,
      String situacion,
      String franja,
      String comparar) {

    public Propuesta {
      sugeridaPor = sugeridaPor == null ? List.of() : List.copyOf(sugeridaPor);
    }

    /** Si se puede aprobar: la que ya no alcanza solo ofrece Descartar. */
    public boolean aprobable() {
      return !"sin-stock".equals(situacion);
    }
  }

  /** Un algoritmo que la sugirió: su nombre llano y su puesto con el total ("3.º de 10"). */
  public record Sugerencia(String algoritmo, String puesto) {}

  /** Una propuesta decidida: {@code estado} es "Aprobada" o "Descartada". */
  public record Decidida(String entidad, String que, String estado) {}

  /**
   * Una donación que está en alguna propuesta pendiente (Destino): {@code que} ("Arroz · 25 kg"),
   * de quién (null si el donante lo ocultó), cuánto hace que está en el depósito, en cuántas
   * pendientes está y si alcanza para todas. {@code resumen}: "La piden 2 propuestas y no alcanza
   * para todas."
   */
  public record DonacionEnJuego(
      String clave,
      String que,
      String deQuien,
      String desde,
      long propuestas,
      boolean alcanza,
      String resumen) {}

  /** Una opción de un filtro: el valor de la dirección y su texto. */
  public record Opcion(String valor, String texto) {}
}
