package grupo5.cliente.vistas;

/**
 * El tablero del inicio de la empresa (R11-25, ficha docs/interfaz/flujos/r11-p2-tablero.md): tres
 * columnas, cada una una página de {@link #POR_COLUMNA} donaciones con su total, pedida al
 * servidor. Una columna en null no se pudo leer: esa columna lo dice y las otras se ven.
 *
 * @param tener "Para tener en cuenta": las que tuvieron un problema
 * @param camino "En camino o por salir": las que van en camino y las asignadas que esperan el
 *     camión
 * @param llego "Ya llegó": las que la organización confirmó
 * @param esperanDestino cuántas siguen en el depósito sin destino (una cifra, no una lista)
 */
public record DonanteTableroVista(
    Tramo<DonanteDonacionVista> tener,
    Tramo<DonanteDonacionVista> camino,
    Tramo<DonanteDonacionVista> llego,
    long esperanDestino) {

  /** Cuántas donaciones muestra cada columna por página. */
  public static final int POR_COLUMNA = 3;

  /**
   * Las entregas en curso hoy (en camino o por salir, más las que tuvieron un problema), o null si
   * alguna de las dos columnas no se pudo leer.
   */
  public Long enCurso() {
    return tener == null || camino == null ? null : tener.total() + camino.total();
  }
}
