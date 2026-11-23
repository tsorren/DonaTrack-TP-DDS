package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entregas de la organización (RF-11 y RF-14; guía al código, ORG-07, DA-02): lo que espera su
 * confirmación, lo que falta llegar y lo que ya confirmó, en una sola pantalla, con la barra del
 * componente listado (la fecha de registro, el estado, la categoría, quien donó si la fuente lo
 * filtra, y el orden) y la última noticia de cada entrega de la página. Antes eran dos pantallas,
 * Donaciones recibidas y Seguir entregas: {@code /entidad-recibidas} redirige acá ({@link
 * RecibidasController}) con los mismos parámetros. Y la entrega que no pudo llegar (ORG-08), con su
 * página. Sin entidad, manda a ingresar. Los datos reales llegan de a una página (R11-43): el
 * filtro por estado lo resuelve Donaciones; la maqueta manda la lista entera y filtra la plantilla.
 */
@Controller
public class EntidadSeguimientoController {

  static final String RUTA = "/entidad-seguimiento";

  private static final String NOMBRE_RECIBIDAS = "recibidas";

  /** Cada valor de {@code ?estado=} y su grupo; uno desconocido no filtra. */
  static final Map<String, String> GRUPOS_POR_ESTADO =
      Map.of(
          "en-tu-puerta",
          RecibidasVista.PUERTA,
          "en-camino",
          RecibidasVista.CAMINO,
          "por-salir",
          RecibidasVista.SALIR,
          "con-problema",
          RecibidasVista.PROBLEMA,
          NOMBRE_RECIBIDAS,
          RecibidasVista.RECIBIDA);

  /** Los valores de {@code ?estado=} en el orden del desplegable. */
  private static final List<Map.Entry<String, String>> VALORES =
      List.of(
          Map.entry("en-tu-puerta", RecibidasVista.PUERTA),
          Map.entry("con-problema", RecibidasVista.PROBLEMA),
          Map.entry("en-camino", RecibidasVista.CAMINO),
          Map.entry("por-salir", RecibidasVista.SALIR),
          Map.entry(NOMBRE_RECIBIDAS, RecibidasVista.RECIBIDA));

  /** El nombre de cada grupo en el desplegable. */
  private static final Map<String, String> NOMBRES =
      Map.of(
          RecibidasVista.PUERTA, "Esperan tu confirmación",
          RecibidasVista.CAMINO, "En camino",
          RecibidasVista.SALIR, "Pendientes",
          RecibidasVista.PROBLEMA, "Con problema",
          RecibidasVista.RECIBIDA, "Confirmadas");

  private static final String SIN_RESULTADOS = "sin-resultados";

  private final RecibidasPuerto recibidas;
  private final EntidadSeguimientoPuerto seguimientos;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadSeguimientoController(
      RecibidasPuerto recibidas,
      EntidadSeguimientoPuerto seguimientos,
      CuentaActual cuentas,
      Pantallas pantallas) {
    this.recibidas = recibidas;
    this.seguimientos = seguimientos;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping(RUTA)
  String entregas(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("entidad-seguimiento", model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    boolean sinResultados = SIN_RESULTADOS.equals(pedido.getParameter("escenario"));
    Set<String> grupos = grupos(pedido);
    if (grupos.isEmpty() && sinResultados) {
      grupos = Set.of(RecibidasVista.PROBLEMA);
    }
    LocalDate[] rango = Listado.rango(pedido.getParameter("desde"), pedido.getParameter("hasta"));
    boolean conDonante = recibidas.filtraPorDonante();
    List<Listado.Opcion> categorias = recibidas.categorias();
    String pedidaCategoria = pedido.getParameter("categoria");
    String categoria =
        categorias.stream().anyMatch(c -> c.valor().equals(pedidaCategoria))
            ? pedidaCategoria
            : null;
    RecibidasPuerto.Consulta consulta =
        new RecibidasPuerto.Consulta(
            grupos,
            Paginadores.pedida(pedido),
            rango[0],
            rango[1],
            categoria,
            conDonante ? pedido.getParameter("quien") : null,
            pedido.getParameter("orden"));
    RecibidasVista datos = recibidas.recibidas(entidad.get(), consulta);
    model.addAttribute(NOMBRE_RECIBIDAS, datos);
    model.addAttribute("listado", listado(consulta, datos, categorias, conDonante, sinResultados));
    model.addAttribute("paginador", Paginadores.de(pedido, "Páginas de entregas", datos.tramo()));
    model.addAttribute(
        "noticiasDeEntregas",
        seguimientos.noticias(
            entidad.get(),
            datos.asignadas().stream()
                .filter(r -> !RecibidasVista.RECIBIDA.equals(r.grupo()))
                .map(Recibida::clave)
                .toList()));
    return vista;
  }

  /**
   * La entrega que no pudo llegar (ORG-08, arquetipo A11): la pedida en {@code ?entrega=}, o la
   * primera con problema si no se pide ninguna. Sin una entrega con problema con esa clave, la
   * plantilla lo dice y vuelve a Entregas. La nueva salida no llega del backend (HG-01).
   */
  @GetMapping("/entidad-entrega-problema")
  String problema(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("entidad-entrega-problema", model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    RecibidasVista datos =
        recibidas.recibidas(
            entidad.get(), RecibidasPuerto.Consulta.de(Set.of(RecibidasVista.PROBLEMA), 1));
    String pedida = pedido.getParameter("entrega");
    Recibida entrega =
        datos.asignadas().stream()
            .filter(r -> RecibidasVista.PROBLEMA.equals(r.grupo()))
            .filter(r -> pedida == null || pedida.equals(r.clave()))
            .findFirst()
            .orElse(null);
    model.addAttribute(NOMBRE_RECIBIDAS, datos);
    model.addAttribute("entrega", entrega);
    model.addAttribute(
        "noticia",
        entrega == null
            ? null
            : seguimientos.noticias(entidad.get(), List.of(entrega.clave())).get(entrega.clave()));
    return vista;
  }

  /**
   * La barra (R11-B04): la fecha de registro, el estado (con cuántas hay en cada uno), la
   * categoría, quien donó (si la fuente lo filtra) y el orden. Con varios estados pedidos (como
   * llega desde Necesidades), una opción más que los junta.
   */
  private static Listado listado(
      RecibidasPuerto.Consulta consulta,
      RecibidasVista datos,
      List<Listado.Opcion> categorias,
      boolean conDonante,
      boolean sinResultados) {
    List<Listado.Opcion> estados = new ArrayList<>();
    estados.add(new Listado.Opcion("", "Todos"));
    List<String> pedidos = new ArrayList<>();
    List<String> nombres = new ArrayList<>();
    for (Map.Entry<String, String> e : VALORES) {
      String grupo = e.getValue();
      long cuantas =
          datos.cuantas() != null
              ? datos.cuantas().getOrDefault(grupo, 0L)
              : datos.asignadas().stream()
                  .filter(r -> grupo.equals(r.grupo()))
                  .filter(r -> !sinResultados || !"aceite".equals(r.clave()))
                  .count();
      String nombre =
          RecibidasVista.RECIBIDA.equals(grupo) && datos.cuantas() == null
              ? "Confirmadas en " + datos.mes()
              : NOMBRES.get(grupo);
      estados.add(new Listado.Opcion(e.getKey(), nombre + " (" + cuantas + ")"));
      if (consulta.grupos().contains(grupo)) {
        pedidos.add(e.getKey());
        nombres.add(nombre);
      }
    }
    String elegido = String.join(",", pedidos);
    if (pedidos.size() > 1) {
      estados.add(new Listado.Opcion(elegido, String.join(" y ", nombres)));
    }
    Listado.Armado barra =
        Listado.en("entregas", RUTA, "Filtrar las entregas")
            .oculto("escenario", sinResultados ? SIN_RESULTADOS : null)
            .fecha("Registrada", consulta.desde(), consulta.hasta())
            .estado(elegido, estados)
            .categoria(
                consulta.categoria() == null ? "" : consulta.categoria(),
                Stream.concat(Stream.of(new Listado.Opcion("", "Todas")), categorias.stream())
                    .toList());
    if (conDonante) {
      barra.quien(
          "Donante", consulta.donante() == null ? "" : consulta.donante(), "Por ejemplo: Almacén");
    }
    return barra
        .orden(
            consulta.orden(),
            List.of(
                new Listado.Opcion("", "Lo que hay que atender primero"),
                new Listado.Opcion(RecibidasPuerto.Consulta.RECIENTES, "Más recientes primero"),
                new Listado.Opcion(Listado.ANTIGUAS, "Más antiguas primero")))
        .sacar(RUTA)
        .listo();
  }

  /**
   * Los grupos pedidos en {@code ?estado=} (repetible, o varios separados por coma, como los manda
   * la opción que junta varios): vacío si no se pidió ninguno que exista.
   */
  static Set<String> grupos(HttpServletRequest pedido) {
    String[] estados = pedido.getParameterValues("estado");
    if (estados == null) {
      return Set.of();
    }
    return Arrays.stream(estados)
        .flatMap(e -> Arrays.stream(e.split(",")))
        .map(e -> GRUPOS_POR_ESTADO.get(e.strip()))
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }
}
