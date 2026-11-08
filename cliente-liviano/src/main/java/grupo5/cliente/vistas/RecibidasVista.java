package grupo5.cliente.vistas;

import java.util.List;

/**
 * Donaciones recibidas (RF-11): lo asignado a la entidad, en el orden de lo que hay que hacer, con
 * las recibidas del mes ({@code mes}, "octubre") al final. {@code actualizado} es cuándo se leyó
 * ("15 de octubre de 2026, 18:30") y {@code novedades}, cuántas notificaciones hay sin leer, o null
 * si no se sabe.
 */
public record RecibidasVista(
    List<Recibida> asignadas, String mes, String actualizado, Integer novedades) {

  /**
   * Una donación asignada. {@code grupo} es {@code puerta}, {@code problema}, {@code camino},
   * {@code salir} o {@code recibida}; {@code categoria} es el ícono del sprite, o null; {@code
   * pedido} y {@code pedidoNombre}, la necesidad que cubre, o null (HB-17). {@code dia} y {@code
   * fecha} (ISO), solo en las recibidas.
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
      String fecha) {}
}
