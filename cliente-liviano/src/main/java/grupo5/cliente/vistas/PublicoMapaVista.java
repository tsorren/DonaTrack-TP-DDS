package grupo5.cliente.vistas;

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
 */
public record PublicoMapaVista(
    List<Estacion> estaciones,
    String rango,
    String actualizado,
    boolean conEntidades,
    boolean conCategorias,
    String nota) {

  /** Todas las entregas del mapa, estación por estación. */
  public List<Entrega> todas() {
    return estaciones.stream().flatMap(e -> e.entregas().stream()).toList();
  }

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
      List<Entrega> entregas) {}

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
