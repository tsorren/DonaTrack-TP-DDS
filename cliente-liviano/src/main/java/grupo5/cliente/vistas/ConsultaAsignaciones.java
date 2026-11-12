package grupo5.cliente.vistas;

import java.util.Set;

/**
 * Qué pide el admin en Asignar donaciones (R11-24), ya entendido: {@code pestana} es {@code
 * propuestas}, {@code conflictos}, {@code destino} o {@code decididas} (Todas las decididas);
 * {@code algoritmo}, el filtro "Ver" de Propuestas ({@code parecido}, {@code menos-recibieron},
 * {@code las-dos} o '', todas); {@code categoria}, el filtro de Destino (la clave de la categoría o
 * ''); {@code donacion}, la donación elegida en Destino (o ''); {@code pagina}, desde 1. Un valor
 * que no se entiende es el de siempre.
 */
public record ConsultaAsignaciones(
    String pestana, String algoritmo, String categoria, String donacion, int pagina) {

  public static final String PROPUESTAS = "propuestas";
  public static final String CONFLICTOS = "conflictos";
  public static final String DESTINO = "destino";
  public static final String DECIDIDAS = "decididas";
  public static final String PARECIDO = "parecido";
  public static final String MENOS_RECIBIERON = "menos-recibieron";
  public static final String LAS_DOS = "las-dos";

  private static final Set<String> PESTANAS = Set.of(PROPUESTAS, CONFLICTOS, DESTINO);
  private static final Set<String> ALGORITMOS = Set.of(PARECIDO, MENOS_RECIBIERON, LAS_DOS);
  private static final String CLAVE = "[A-Za-z0-9-]{1,64}";

  public ConsultaAsignaciones {
    pestana = PESTANAS.contains(pestana) || DECIDIDAS.equals(pestana) ? pestana : PROPUESTAS;
    algoritmo = ALGORITMOS.contains(algoritmo) ? algoritmo : "";
    categoria = categoria != null && categoria.matches(CLAVE) ? categoria : "";
    donacion = donacion != null && donacion.matches(CLAVE) ? donacion : "";
    pagina = Math.max(1, pagina);
  }

  /** La pestaña de entrada, sin filtros. */
  public static ConsultaAsignaciones primera() {
    return new ConsultaAsignaciones(PROPUESTAS, "", "", "", 1);
  }

  /**
   * Lo que llegó en la dirección: {@code ?pestana=}, {@code ?algoritmo=}, {@code ?categoria=},
   * {@code ?donacion=} y {@code ?vista=decididas} (Todas las decididas, que manda sobre la
   * pestaña).
   */
  public static ConsultaAsignaciones de(
      String pestana,
      String vista,
      String algoritmo,
      String categoria,
      String donacion,
      int pagina) {
    String elegida = DECIDIDAS.equals(vista) ? DECIDIDAS : vacio(pestana);
    return new ConsultaAsignaciones(
        elegida, vacio(algoritmo), vacio(categoria), vacio(donacion), pagina);
  }

  /** En Destino, con una donación elegida (paso 2). */
  public boolean conDonacion() {
    return DESTINO.equals(pestana) && !donacion.isEmpty();
  }

  private static String vacio(String valor) {
    return valor == null ? "" : valor.strip();
  }
}
