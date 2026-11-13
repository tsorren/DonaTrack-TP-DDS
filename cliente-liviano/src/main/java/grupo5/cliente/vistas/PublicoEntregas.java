package grupo5.cliente.vistas;

import java.util.List;

/**
 * Las entregas confirmadas del último mes, la más reciente primero, con los textos del período ya
 * escritos: {@code rangoMes} ("del 16 de septiembre al 15 de octubre de 2026"), {@code rangoSemana}
 * y {@code actualizado} ("15 de octubre de 2026, 14:30").
 */
public record PublicoEntregas(
    List<Entrega> lista, String rangoMes, String rangoSemana, String actualizado) {

  /**
   * Una entrega confirmada. {@code que}: "Sillas, 6 unidades"; {@code cat}: la clave del ícono
   * ({@code muebles}), o null si la categoría no tiene ícono; {@code categoria}: su nombre; {@code
   * lugar}: la entidad y, si se sabe, el barrio; {@code dia} ("14 de octubre") y {@code anio};
   * {@code fecha}, ISO; {@code semana}, si entra en la última semana; {@code foto} y {@code alt},
   * la foto que subió la entidad.
   */
  public record Entrega(
      String que,
      String cat,
      String categoria,
      String entidad,
      String lugar,
      String dia,
      String anio,
      String fecha,
      boolean semana,
      String foto,
      String alt) {}
}
