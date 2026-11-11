package grupo5.cliente.vistas;

import java.util.List;

/**
 * Asignar donaciones del admin (RF-17): la última asignación ("hoy, 0:00 · generó 2 propuestas", o
 * null si nunca corrió), las propuestas que esperan una decisión y las ya decididas.
 */
public record AdminAsignacionesVista(
    String ultima, List<Propuesta> propuestas, List<Decidida> decididas) {

  public AdminAsignacionesVista {
    propuestas = List.copyOf(propuestas);
    decididas = List.copyOf(decididas);
  }

  /**
   * Una propuesta pendiente: quién pide qué, cuándo se propuso ({@code nueva}: hoy), qué donaciones
   * la cubren y cuánto.
   */
  public record Propuesta(
      String clave,
      String entidad,
      String pide,
      String cuando,
      String cubre,
      String alcance,
      boolean nueva) {}

  /** Una propuesta decidida: {@code estado} es "Aprobada" o "Descartada". */
  public record Decidida(String entidad, String que, String estado) {}
}
