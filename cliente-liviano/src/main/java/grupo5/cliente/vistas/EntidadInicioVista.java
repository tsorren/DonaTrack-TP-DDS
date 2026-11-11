package grupo5.cliente.vistas;

import java.util.List;

/**
 * El Inicio de la entidad, además de lo asignado (que llega en {@link RecibidasVista}): {@code
 * fecha}, el día en palabras ("jueves 15 de octubre de 2026"); {@code al}, cuándo se leyó ("15 de
 * octubre, 18:30"); y las necesidades vigentes. Las novedades sin leer son las de la cabecera.
 */
public record EntidadInicioVista(String fecha, String al, List<Necesidad> necesidades) {

  /**
   * Una necesidad vigente. {@code categoria}, el ícono del sprite, o null; {@code recibido}, lo que
   * llegó sobre lo pedido ("10 de 40 kg"), con {@code valor} y {@code max} para la barra; {@code
   * recibido} y {@code valor} son null si no se sabe. {@code plazo}, hasta cuándo, o null.
   */
  public record Necesidad(
      String nombre, String categoria, String recibido, Integer valor, Integer max, String plazo) {}
}
