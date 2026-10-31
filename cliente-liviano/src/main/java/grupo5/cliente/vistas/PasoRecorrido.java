package grupo5.cliente.vistas;

import java.util.Locale;

/**
 * Un paso del recorrido de una donación. {@code detalle} dice el estado en palabras (una fecha o
 * "Pendiente"), para que el color no sea la única señal.
 */
public record PasoRecorrido(String nombre, String detalle, Avance avance) {

  /** Dónde está la donación respecto de este paso. */
  public enum Avance {
    HECHO,
    ACTUAL,
    PENDIENTE;

    /** Sufijo de la clase CSS del paso: {@code recorrido-paso--actual}. */
    public String tono() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
