package grupo5.cliente.vistas;

import java.util.List;

/**
 * Entidades del admin (RF-21 y RF-15): una sola lista, por nombre, con las que piden sumarse, las
 * activas y las inactivas. {@code activas} cuenta todas las activas (la lista puede traer menos) y
 * {@code al} dice cuándo se leyó ("15 de octubre de 2026, 18:30").
 */
public record AdminEntidadesVista(long activas, String al, List<Entidad> todas) {

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
