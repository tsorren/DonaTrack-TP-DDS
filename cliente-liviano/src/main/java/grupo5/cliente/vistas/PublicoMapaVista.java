package grupo5.cliente.vistas;

import java.util.ArrayList;
import java.util.List;

/**
 * El mapa de entregadas del último mes: un punto (una estación) por lugar, con sus entregas. Lo que
 * la API no da viaja como null o como un indicador apagado, y la pantalla lo dice o lo oculta.
 *
 * @param rango el período, ya escrito ("del 16 de septiembre al 15 de octubre de 2026")
 * @param actualizado cuándo se leyó ("15 de octubre de 2026, 14:30")
 * @param conEntidades si cada punto es una entidad (con su nombre y lo que pide); si no, es un
 *     lugar donde se confirmaron entregas
 * @param conCategorias si las entregas traen su categoría (el filtro del mapa y los íconos)
 * @param nota lo que la pantalla avisa que todavía no muestra, o null si lo muestra todo
 * @param delPeriodo cuántas entregas confirmadas hay en el período, sin el tope del mapa (el {@code
 *     X-Total-Count} de Logística, R11-19); si es mayor que las que trae, el mapa muestra solo las
 *     más recientes y la cifra lo dice
 */
public record PublicoMapaVista(
    List<Estacion> estaciones,
    String rango,
    String actualizado,
    boolean conEntidades,
    boolean conCategorias,
    String nota,
    long delPeriodo) {

  /** Filas por página de la lista del mapa (R11-19): diez plegadas ocupan el alto del mapa. */
  public static final int POR_PAGINA = 10;

  public PublicoMapaVista {
    estaciones = List.copyOf(estaciones);
  }

  /** Sin tope: el total del período es lo que trae (la maqueta y los tests). */
  public PublicoMapaVista(
      List<Estacion> estaciones,
      String rango,
      String actualizado,
      boolean conEntidades,
      boolean conCategorias,
      String nota) {
    this(
        estaciones,
        rango,
        actualizado,
        conEntidades,
        conCategorias,
        nota,
        estaciones.stream().mapToLong(e -> e.entregas().size()).sum());
  }

  /** Todas las entregas del mapa, estación por estación. */
  public List<Entrega> todas() {
    return estaciones.stream().flatMap(e -> e.entregas().stream()).toList();
  }

  /** Si el período tiene más entregas que las que muestra el mapa (el tope de Logística). */
  public boolean conTope() {
    return delPeriodo > todas().size();
  }

  /**
   * La lista del mapa con las entregas de {@code categoria} (todas, si es null o vacía): todos los
   * puntos del período, cada uno con su página, y la página {@code pedida} (la última que hay, si
   * se pasa) de {@link #POR_PAGINA} filas. Una estación sin entregas de la categoría no va.
   */
  public Lista lista(String categoria, int pedida) {
    List<Estacion> conEntregas = new ArrayList<>();
    for (Estacion estacion : estaciones) {
      List<Entrega> suyas =
          categoria == null || categoria.isBlank()
              ? estacion.entregas()
              : estacion.entregas().stream().filter(e -> categoria.equals(e.cat())).toList();
      if (!suyas.isEmpty()) {
        conEntregas.add(estacion.con(suyas));
      }
    }
    List<Punto> puntos = new ArrayList<>();
    for (int i = 0; i < conEntregas.size(); i++) {
      puntos.add(new Punto(conEntregas.get(i), i / POR_PAGINA + 1));
    }
    return new Lista(puntos, Tramo.de(conEntregas, pedida, POR_PAGINA));
  }

  /**
   * Lo que dibuja la pantalla: {@code puntos}, todos los lugares del período para el mapa (con la
   * página de la lista en que está cada uno), y {@code pagina}, las filas de la lista que se ven.
   */
  public record Lista(List<Punto> puntos, Tramo<Estacion> pagina) {

    public Lista {
      puntos = List.copyOf(puntos);
    }

    /** Cuántas entregas suman los puntos. */
    public int entregas() {
      return puntos.stream().mapToInt(p -> p.estacion().entregas().size()).sum();
    }
  }

  /** Un punto del mapa y la página de la lista en la que está su fila. */
  public record Punto(Estacion estacion, int pagina) {}

  /**
   * Un punto del mapa. {@code numero}: el que lleva en el mapa y en la lista; {@code clave}: su
   * ancla ({@code entidad-<clave>}); {@code nombre} y {@code barrio}: la entidad y su barrio, o el
   * lugar y su provincia; {@code pide}: lo que pide la entidad (vacío si no se sabe).
   */
  public record Estacion(
      String clave,
      int numero,
      double lat,
      double lng,
      String nombre,
      String barrio,
      List<String> pide,
      List<Entrega> entregas) {

    /** La misma estación con otras entregas (las de una categoría). */
    Estacion con(List<Entrega> otras) {
      return new Estacion(clave, numero, lat, lng, nombre, barrio, pide, otras);
    }
  }

  /**
   * Una entrega confirmada. {@code cat}: la clave del ícono ({@code muebles}) y {@code categoria}:
   * su nombre, o null si no se sabe; {@code dia} ("14 de octubre"), {@code anio} y {@code fecha}
   * (ISO); {@code entidad}: el nombre de su estación; {@code foto} y {@code alt}: la foto de la
   * entrega, o null si no hay (sin ella, no hay "Ver la foto").
   */
  public record Entrega(
      String que,
      String cat,
      String categoria,
      String dia,
      String anio,
      String fecha,
      String entidad,
      String foto,
      String alt) {}
}
