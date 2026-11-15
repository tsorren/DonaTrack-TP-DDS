package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.RecibidasVista;
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
 * Donaciones recibidas de la entidad (RF-11), con los datos del puerto. Sin entidad todavía, manda
 * a ingresar, como un 401 de la API. Desde R11-43 los datos reales llegan de a una página: el
 * filtro por estado ({@code ?estado=}, repetible) lo resuelve Donaciones y la plantilla dibuja el
 * paginador; la maqueta manda la lista entera y filtra la plantilla. Desde R11-B04, la barra del
 * componente listado ({@code listado}) suma la fecha de registro ({@code ?desde=}, {@code
 * ?hasta=}), la categoría, quien donó ({@code ?quien=}, solo si la fuente lo filtra) y el orden.
 */
@Controller
public class RecibidasController {

  private static final String RECIBIDAS = "recibidas";

  /** Cada valor de {@code ?estado=} y su grupo; uno desconocido no filtra. */
  static final Map<String, String> GRUPOS =
      Map.of(
          "en-tu-puerta",
          RecibidasVista.PUERTA,
          "en-camino",
          RecibidasVista.CAMINO,
          "por-salir",
          RecibidasVista.SALIR,
          "con-problema",
          RecibidasVista.PROBLEMA,
          RECIBIDAS,
          RecibidasVista.RECIBIDA);

  /** Los valores de {@code ?estado=} en el orden del desplegable. */
  private static final List<Map.Entry<String, String>> VALORES =
      List.of(
          Map.entry("en-tu-puerta", RecibidasVista.PUERTA),
          Map.entry("en-camino", RecibidasVista.CAMINO),
          Map.entry("por-salir", RecibidasVista.SALIR),
          Map.entry("con-problema", RecibidasVista.PROBLEMA),
          Map.entry(RECIBIDAS, RecibidasVista.RECIBIDA));

  /** El nombre de cada grupo en el desplegable. */
  private static final Map<String, String> NOMBRES =
      Map.of(
          RecibidasVista.PUERTA, "Esperan tu confirmación",
          RecibidasVista.CAMINO, "En camino",
          RecibidasVista.SALIR, "Pendientes",
          RecibidasVista.PROBLEMA, "Con problema",
          RecibidasVista.RECIBIDA, "Recibidas");

  private static final String SIN_RESULTADOS = "sin-resultados";

  private final RecibidasPuerto recibidas;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  RecibidasController(RecibidasPuerto recibidas, CuentaActual cuentas, Pantallas pantallas) {
    this.recibidas = recibidas;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/entidad-recibidas")
  String recibidas(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("entidad-recibidas", model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return "redirect:/publico-ingresar";
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
    model.addAttribute(RECIBIDAS, datos);
    model.addAttribute("listado", listado(consulta, datos, categorias, conDonante, sinResultados));
    model.addAttribute("paginador", Paginadores.de(pedido, "Páginas de donaciones", datos.tramo()));
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
              ? "Recibidas en " + datos.mes()
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
        Listado.en(RECIBIDAS, "/entidad-recibidas", "Filtrar las donaciones recibidas")
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
        .sacar("/entidad-recibidas")
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
        .map(e -> GRUPOS.get(e.strip()))
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }
}
