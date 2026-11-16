package grupo5.cliente.vistas;

import java.util.List;

/**
 * Entidades y necesidades (público): las entidades con pedidos vigentes. Lo que la API no da viaja
 * como null o como un indicador apagado, y la pantalla lo dice o lo oculta.
 *
 * @param total las entidades activas de DonaTrack (no solo las que tienen pedidos)
 * @param fecha el día de los datos ("15 de octubre de 2026")
 * @param periodo el mes de «Necesidades satisfechas» ("octubre de 2026"), o null si no se cuenta
 * @param conCategorias si los pedidos traen su categoría (el filtro por categoría y los íconos)
 * @param conEmergencias si se sabe qué pedidos son una emergencia (el filtro «Emergencias»)
 * @param nota lo que la pantalla avisa que todavía no muestra, o null si lo muestra todo
 */
public record PublicoEntidadesVista(
    List<Entidad> entidades,
    long total,
    String fecha,
    String periodo,
    boolean conCategorias,
    boolean conEmergencias,
    String nota) {

  /**
   * Una entidad con pedidos. {@code tipo}: Comedor, Merendero, Escuela, Biblioteca, Hogar, Club u
   * Otra; {@code cat}: la clave del ícono de su categoría ({@code alimentos}), o null; {@code
   * numero}: su estación en el mapa, o null si no se la enlaza; {@code cubiertas}: las necesidades
   * del período ya cubiertas del todo, o null si no se cuentan; {@code mision} y {@code
   * emergencia}: sus textos, o null si no se tienen.
   */
  public record Entidad(
      String clave,
      String tipo,
      String nombre,
      String cat,
      Integer numero,
      Integer cubiertas,
      String mision,
      String emergencia,
      List<Pedido> pedidos) {}

  /**
   * Un pedido. {@code falta}: lo que falta o lo que se pide, ya en palabras; {@code cuando}: hasta
   * cuándo o si se repite; {@code faltan} y {@code total}: para la barra (sin {@code faltan}, no
   * hay barra); {@code tipo}: {@code recurrente} o {@code extraordinaria}.
   */
  public record Pedido(
      String que,
      String cat,
      String falta,
      String cuando,
      Integer faltan,
      Integer total,
      String tipo,
      boolean emergencia) {}
}
