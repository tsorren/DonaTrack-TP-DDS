package grupo5.cliente.vistas;

import java.util.Locale;

/**
 * Cómo se muestra el estado de una entrega: texto para la persona, ícono (fragmentos/icono) y tono
 * para el color.
 */
public enum EstadoEntrega {
  PENDIENTE("Pendiente", "donaciones"),
  EN_CAMINO("En camino", "camiones"),
  ENTREGADA("Entregada", "check"),
  CON_PROBLEMA("Con problema", "alerta");

  private final String texto;
  private final String icono;

  EstadoEntrega(String texto, String icono) {
    this.texto = texto;
    this.icono = icono;
  }

  public String texto() {
    return texto;
  }

  public String icono() {
    return icono;
  }

  /** Sufijo de la clase CSS de la insignia: {@code insignia--en-camino}. */
  public String tono() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
