package grupo5.cliente.vistas;

import java.util.List;

/**
 * Donaciones del admin (RF-15 y RF-16), de a una página (R11-01). {@code donaciones} es la página
 * que se muestra, ya filtrada y ordenada por la fuente, con su total; {@code consulta}, el filtro y
 * el orden que quedaron aplicados (un orden que la fuente no ofrece se toma como el de partida);
 * {@code criterios}, los órdenes que se pueden elegir (el primero es el de partida, valor '');
 * {@code conVence}, si la fuente filtra "vencen en los próximos 7 días" (si no, el filtro no se
 * ofrece y {@code porVencer} dice cuántas vencen); {@code resumen}, las cifras de la cabecera con
 * su fecha.
 */
public record AdminDonacionesVista(
    Tramo<Donacion> donaciones,
    ConsultaDonaciones consulta,
    List<Criterio> criterios,
    boolean conVence,
    String porVencer,
    String resumen) {

  public AdminDonacionesVista {
    criterios = List.copyOf(criterios);
  }

  /** Un orden de la lista: su valor en la dirección, su texto y el título de la tabla. */
  public record Criterio(String valor, String texto, String caption) {}

  /** {@code valor} si es uno de {@code criterios}; si no, '' (el de partida). */
  public static String elegido(String valor, List<Criterio> criterios) {
    return valor != null && criterios.stream().anyMatch(c -> c.valor().equals(valor)) ? valor : "";
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
