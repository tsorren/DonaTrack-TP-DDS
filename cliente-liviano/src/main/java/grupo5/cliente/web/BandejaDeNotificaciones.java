package grupo5.cliente.web;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.Listado;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Lo común de las bandejas de notificaciones del donante y de la entidad (R11-14, ficha
 * docs/interfaz/flujos/r11-p1-notificaciones.md): la consulta que viene en la dirección y, después
 * de marcar, la vuelta a la misma página con los mismos filtros (el formulario del buzón los lleva
 * en su {@code action}).
 */
final class BandejaDeNotificaciones {

  /** El nombre del paginador para los lectores de pantalla (ficha, sección 6). */
  static final String PAGINAS = "Páginas de notificaciones";

  /**
   * Lo que la bandeja conserva en la dirección: los filtros (con los nombres de antes de R11-B04
   * también), el agrupado y la página.
   */
  private static final List<String> CONSERVA =
      List.of(
          "estado",
          "ver",
          "buscar",
          "desde",
          "hasta",
          "categoria",
          "tema",
          "orden",
          "agrupar",
          Paginadores.PAGINA);

  /** El valor de {@code ?agrupar=} que agrupa por fecha; sin él, la lista va sin agrupar. */
  static final String POR_FECHA = "fecha";

  private BandejaDeNotificaciones() {}

  /**
   * La consulta de la dirección (R11-B04: {@code estado}, {@code categoria}, {@code desde} y {@code
   * hasta}; {@code ver} y {@code tema} siguen valiendo). Sin {@code conFecha} (la fuente no filtra
   * por fecha), las fechas no se piden.
   */
  static ConsultaBandeja consulta(HttpServletRequest pedido, boolean conFecha) {
    return ConsultaBandeja.de(
        primero(pedido, "estado", "ver"),
        pedido.getParameter("buscar"),
        primero(pedido, "categoria", "tema"),
        pedido.getParameter("orden"),
        conFecha ? pedido.getParameter("desde") : null,
        conFecha ? pedido.getParameter("hasta") : null,
        Paginadores.pedida(pedido));
  }

  /**
   * La barra de la bandeja (R11-B04, componente listado): la búsqueda, la fecha del aviso (si la
   * fuente la filtra), estado (sin leer o leídas), categoría, "Agrupar" y el orden. Las
   * notificaciones no tienen un donante o una organización propios: "quién" no va. {@code temas}
   * son las categorías del rol, la primera "Todas las categorías". La categoría pedida va tal cual:
   * si no es una de ellas, no cuenta como elegida (la bandeja de la persona de la maqueta pone las
   * suyas con {@link Listado#conCategorias}).
   */
  static Listado listado(
      String ruta,
      ConsultaBandeja c,
      List<Listado.Opcion> temas,
      boolean conFecha,
      String agrupar) {
    Listado.Armado barra =
        Listado.en("notificaciones", ruta, "Buscar y filtrar las notificaciones")
            .buscar("Buscar en las notificaciones", c.buscar(), "Por ejemplo: camión");
    if (conFecha) {
      barra.fecha("Fecha", c.desde(), c.hasta());
    }
    return barra
        .estado(
            c.estado(),
            List.of(
                new Listado.Opcion("", "Todas"),
                new Listado.Opcion(ConsultaBandeja.SIN_LEER, "Sin leer"),
                new Listado.Opcion(ConsultaBandeja.ESTADO_LEIDAS, "Leídas")))
        .categoria(c.tema(), temas)
        .otra(
            Listado.Eleccion.deVista(
                "agrupar",
                "Agrupar",
                POR_FECHA.equals(agrupar) ? POR_FECHA : "",
                List.of(
                    new Listado.Opcion("", "Sin agrupar"),
                    new Listado.Opcion(POR_FECHA, "Por fecha"))))
        .orden(c.viejas() ? Listado.ANTIGUAS : "", Listado.POR_FECHA)
        .sacar(ruta)
        .listo();
  }

  private static String primero(HttpServletRequest pedido, String nombre, String antes) {
    String valor = pedido.getParameter(nombre);
    return valor != null ? valor : pedido.getParameter(antes);
  }

  /**
   * La pantalla con los filtros y la página que traía el pedido, para {@link Envios#listo}: {@code
   * donante-notificaciones?ver=sin-leer&pagina=2}.
   */
  static String volver(String pantalla, HttpServletRequest pedido) {
    UriComponentsBuilder direccion = UriComponentsBuilder.fromPath(pantalla);
    for (String nombre : CONSERVA) {
      String valor = pedido.getParameter(nombre);
      if (valor != null) {
        direccion.queryParam(nombre, valor);
      }
    }
    return direccion.encode().build().toUriString();
  }
}
