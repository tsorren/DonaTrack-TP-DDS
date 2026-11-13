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
 * Lo que no corresponde a la pestaña es null o vacío. En Todas las decididas (R11-35), {@code
 * autores} son las opciones de "Decidió" (la primera, "Cualquiera") y {@code sinFecha}, cuántas
 * decididas no tienen fecha guardada (un número de Donaciones).
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
    DonacionEnJuego elegida,
    List<Opcion> autores,
    long sinFecha) {

  /** Los nombres de los algoritmos en pantalla (ficha, sección 6). */
  public static final String PARECIDO = "Parecido con lo pedido";

  public static final String MENOS_RECIBIERON = "Quienes menos recibieron";

  public AdminAsignacionesVista {
    recientes = recientes == null ? List.of() : List.copyOf(recientes);
    categorias = categorias == null ? List.of() : List.copyOf(categorias);
    autores = autores == null ? List.of() : List.copyOf(autores);
  }

  /** Sin lo de Todas las decididas de R11-35. */
  public AdminAsignacionesVista(
      ConsultaAsignaciones consulta,
      Cuentas cuentas,
      String ultima,
      Tramo<Propuesta> propuestas,
      List<Decidida> recientes,
      Tramo<Decidida> decididas,
      List<Opcion> categorias,
      Tramo<DonacionEnJuego> donaciones,
      DonacionEnJuego elegida) {
    this(
        consulta,
        cuentas,
        ultima,
        propuestas,
        recientes,
        decididas,
        categorias,
        donaciones,
        elegida,
        List.of(),
        0);
  }

  /** Las opciones de "Estado" de Todas las decididas. */
  public List<Opcion> estadosDecididas() {
    return List.of(
        new Opcion("", "Todas"),
        new Opcion(ConsultaDecididas.APROBADAS, "Aprobadas"),
        new Opcion(ConsultaDecididas.DESCARTADAS, "Descartadas"));
  }

  /** Las opciones de "Decidida": los períodos fijos. */
  public List<Opcion> periodosDecididas() {
    return ConsultaDecididas.PERIODOS_OPCIONES;
  }

  /** Las opciones del orden de Todas las decididas. */
  public List<Opcion> ordenesDecididas() {
    return List.of(
        new Opcion("", "Más recientes primero"),
        new Opcion(ConsultaDecididas.ANTIGUAS, "Más antiguas primero"));
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

  /**
   * Una propuesta decidida: {@code estado} es "Aprobada" o "Descartada". Desde R11-35: {@code
   * categoria} (la de la necesidad, o null), {@code quien} (la frase con quién y cuándo: "Aprobó
   * Ana Pérez el 12 de noviembre de 2026, 10:15.", o la de las decididas sin dato), {@code motivo}
   * (lo que anotó al descartar; '' si se descartó sin motivo; null si no se muestra: aprobada o en
   * las 3 recientes) y {@code cubria} (las donaciones que la cubrían, con la clave de cada una para
   * su ficha).
   */
  public record Decidida(
      String entidad,
      String que,
      String estado,
      String categoria,
      String quien,
      String motivo,
      List<Cubre> cubria) {

    public Decidida {
      cubria = cubria == null ? List.of() : List.copyOf(cubria);
    }

    /** Sin los datos de R11-35. */
    public Decidida(String entidad, String que, String estado) {
      this(entidad, que, estado, null, null, null, List.of());
    }

    public boolean descartada() {
      return "Descartada".equals(estado);
    }
  }

  /** Una donación que cubría una decidida: su clave y "Arroz · 25 kg (de Ana Ejemplo)". */
  public record Cubre(String clave, String texto) {}

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
