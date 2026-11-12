package grupo5.cliente.vistas;

import java.util.List;

/**
 * Qué página de donaciones pide el admin (R11-01), tal como llega en la dirección: {@code estado}
 * ({@code deposito}, {@code asignada}, {@code camino}, {@code entregada}, {@code vencida} o '' para
 * todas), {@code pronto} (solo las del depósito que vencen en los próximos 7 días), {@code orden}
 * ('' es el de partida de la fuente) y {@code pagina}, desde 1. Un estado que no es de la lista se
 * toma como ''.
 */
public record ConsultaDonaciones(String estado, boolean pronto, String orden, int pagina) {

  /** Los estados que la pantalla filtra, en el orden de su menú. */
  public static final List<String> ESTADOS =
      List.of("deposito", "asignada", "camino", "entregada", "vencida");

  public ConsultaDonaciones {
    estado = estado != null && ESTADOS.contains(estado) && !pronto ? estado : "";
    orden = orden == null ? "" : orden;
    pagina = Math.max(1, pagina);
  }
}
