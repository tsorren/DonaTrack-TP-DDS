package grupo5.cliente.vistas;

import java.util.List;
import java.util.Map;

/**
 * Donaciones del admin (RF-15 y RF-16) y el detalle de una: todas las donaciones, en el orden de
 * partida (primero las del depósito que vencen antes), cada otro orden como la lista de claves
 * ({@code reciente}, {@code antigua}, {@code nombre} y {@code donante}) y las cifras de la cabecera
 * con su fecha.
 */
public record AdminDonacionesVista(
    List<Donacion> todas, Map<String, List<String>> ordenes, String resumen) {

  public AdminDonacionesVista {
    todas = List.copyOf(todas);
    ordenes = Map.copyOf(ordenes);
  }

  /**
   * Una donación. {@code estado} es {@code deposito}, {@code asignada}, {@code camino}, {@code
   * entregada} o {@code vencida}; {@code vence}, la fecha más próxima de sus bienes, o null; {@code
   * prontaAVencer}, si está en el depósito y vence en los próximos 7 días. {@code bienes}, los de
   * una donación de varios bienes (null si tiene uno). {@code historial}, cada cambio ya escrito
   * para la pantalla. {@code nueva}: la recién registrada de la maqueta.
   */
  public record Donacion(
      String clave,
      String que,
      String cantidad,
      String corto,
      String donante,
      String registrada,
      String estado,
      String vence,
      boolean prontaAVencer,
      String bien,
      String deposito,
      String entidad,
      boolean nueva,
      List<Bien> bienes,
      List<String> historial) {

    public Donacion {
      bienes = bienes == null ? null : List.copyOf(bienes);
      historial = List.copyOf(historial);
    }
  }

  /** Un bien de la donación: qué es, cuánto, en qué estado y cuándo vence (o null). */
  public record Bien(String que, String cantidad, String bien, String vence) {}
}
