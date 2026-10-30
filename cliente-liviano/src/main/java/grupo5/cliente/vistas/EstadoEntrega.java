package grupo5.cliente.vistas;

import java.util.Locale;

/** Cómo se muestra el estado de una entrega: texto para la persona y tono para el color. */
public enum EstadoEntrega {
  PENDIENTE("Pendiente"),
  EN_CAMINO("En camino"),
  ENTREGADA("Entregada"),
  CON_PROBLEMA("Con problema");

  private final String texto;

  EstadoEntrega(String texto) {
    this.texto = texto;
  }

  public String texto() {
    return texto;
  }

  /** Sufijo de la clase CSS de la insignia: {@code insignia--en-camino}. */
  public String tono() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
