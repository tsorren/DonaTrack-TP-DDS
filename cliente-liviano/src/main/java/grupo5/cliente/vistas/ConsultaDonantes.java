package grupo5.cliente.vistas;

/**
 * Qué página de donantes pide el admin (R11-01): la búsqueda por nombre o documento y los filtros
 * de la pantalla, tal como llegan en la dirección. {@code tipo}, {@code dona}, {@code estado} y
 * {@code orden} ya están validados contra las opciones que ofrece la vista ('' es "todos" y, en el
 * orden, "más nuevos primero"); {@code pagina} empieza en 1.
 */
public record ConsultaDonantes(
    String buscar, String tipo, String dona, String estado, String orden, int pagina) {

  public ConsultaDonantes {
    buscar = buscar == null ? "" : buscar.strip();
    tipo = tipo == null ? "" : tipo;
    dona = dona == null ? "" : dona;
    estado = estado == null ? "" : estado;
    orden = orden == null ? "" : orden;
    pagina = Math.max(1, pagina);
  }

  /** La primera página, sin búsqueda ni filtros. */
  public static ConsultaDonantes primera() {
    return new ConsultaDonantes("", "", "", "", "", 1);
  }

  /** Si hay búsqueda o algún filtro (el orden no cuenta). */
  public boolean filtrada() {
    return !buscar.isEmpty() || !tipo.isEmpty() || !dona.isEmpty() || !estado.isEmpty();
  }
}
