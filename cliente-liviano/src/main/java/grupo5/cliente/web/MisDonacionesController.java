package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.puertos.DonacionesPuerto.Consulta;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Mis donaciones del donante (RF-05) y el detalle de una, con los datos del puerto. Sin cuenta
 * todavía, manda a ingresar, como un 401 de la API. Los atributos se llaman {@code misDonaciones} y
 * {@code detalle} porque {@code viaje-datos.html} ya define {@code donaciones} para las pantallas
 * que siguen con la maqueta. Una donación que no es del donante es {@code detalle} null: "No
 * encontramos esa donación". Con {@code ?mes=2026-10} ("Ver todas" de un mes del inicio de la
 * persona), solo las registradas ese mes; un mes mal escrito se ignora. El estado, la categoría y
 * la subcategoría ({@code ?subcategoria=}, RF-05, R11-13) y la página ({@code ?pagina=2}, desde 1)
 * los resuelve el puerto: con Donaciones, en el servidor (R11-01); un estado desconocido o una
 * página mal escrita se ignoran. {@code paginador}: el componente paginas, que conserva el mes y
 * los filtros (null con una sola página).
 */
@Controller
public class MisDonacionesController {

  private static final String INGRESAR = "redirect:/publico-ingresar";
  private static final DateTimeFormatter MES =
      DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-AR"));

  private final DonacionesPuerto donaciones;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  MisDonacionesController(DonacionesPuerto donaciones, CuentaActual cuentas, Pantallas pantallas) {
    this.donaciones = donaciones;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-donaciones")
  String misDonaciones(
      @RequestParam(required = false) String mes,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String categoria,
      @RequestParam(required = false) String subcategoria,
      @RequestParam(required = false) String pagina,
      @RequestParam(required = false) String desde,
      @RequestParam(required = false) String hasta,
      @RequestParam(required = false) String quien,
      @RequestParam(required = false) String orden,
      Model model,
      HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    String vista = pantallas.mostrarDonante("donante-donaciones", donante.get(), model, pedido);
    Optional<YearMonth> delMes = mes(mes);
    LocalDate[] rango = Listado.rango(desde, hasta);
    boolean conDestino = donaciones.filtraPorDestino();
    Consulta consulta =
        new Consulta(
            delMes.orElse(null),
            estado(estado),
            texto(categoria),
            texto(subcategoria),
            pagina(pagina),
            rango[0],
            rango[1],
            conDestino ? texto(quien) : null,
            Listado.ANTIGUAS.equals(orden));
    MisDonacionesVista lista = donaciones.misDonaciones(donante.get(), consulta);
    model.addAttribute("misDonaciones", lista);
    model.addAttribute("listado", listado(consulta, lista, conDestino));
    model.addAttribute(
        "paginador",
        Paginadores.de(
            pedido,
            "Páginas de tus donaciones",
            new Tramo<>(lista.donaciones(), lista.total(), lista.pagina(), lista.paginas())));
    model.addAttribute("mesClave", delMes.map(YearMonth::toString).orElse(null));
    model.addAttribute("mesTexto", delMes.map(MES::format).orElse(null));
    return vista;
  }

  /**
   * La barra (R11-B04): la fecha de registro (con {@code ?mes=}, sus días ya puestos), estado,
   * categoría, la organización a la que fue (si la fuente la filtra), la subcategoría y el orden.
   * Una categoría o subcategoría que no es del filtro queda en "Todas".
   */
  private static Listado listado(Consulta consulta, MisDonacionesVista lista, boolean conDestino) {
    List<MisDonacionesVista.Categoria> categorias = lista.categorias();
    String subcategoria = consulta.subcategoriaEn(categorias);
    String categoria = consulta.categoriaEn(categorias);
    Listado.Armado barra =
        Listado.en("donaciones", "/donante-donaciones", "Filtrar tus donaciones")
            .fecha("Registrada", consulta.primerDia(), consulta.ultimoDia())
            .estado(
                consulta.estado() == null ? "" : consulta.estado().tono(),
                List.of(
                    new Listado.Opcion("", "Todos"),
                    new Listado.Opcion(EstadoEntrega.PENDIENTE.tono(), "Pendiente"),
                    new Listado.Opcion(EstadoEntrega.EN_CAMINO.tono(), "En camino"),
                    new Listado.Opcion(EstadoEntrega.ENTREGADA.tono(), "Entregada"),
                    new Listado.Opcion(EstadoEntrega.CON_PROBLEMA.tono(), "Con problema")))
            .categoria(
                categoria == null ? "" : categoria,
                Stream.concat(
                        Stream.of(new Listado.Opcion("", "Todas")),
                        categorias.stream().map(c -> new Listado.Opcion(c.valor(), c.texto())))
                    .toList());
    if (conDestino) {
      barra.quien(
          "Organización",
          consulta.destino() == null ? "" : consulta.destino(),
          "Por ejemplo: Comedor");
    }
    if (categorias.stream().anyMatch(c -> !c.subcategorias().isEmpty())) {
      barra.otra(
          new Listado.Eleccion(
              "subcategoria",
              "Subcategoría",
              subcategoria,
              List.of(new Listado.Opcion("", "Todas")),
              categorias.stream()
                  .filter(c -> !c.subcategorias().isEmpty())
                  .map(
                      c ->
                          new Listado.Grupo(
                              c.texto(),
                              c.subcategorias().stream()
                                  .map(sc -> new Listado.Opcion(sc.valor(), sc.texto()))
                                  .toList()))
                  .toList()));
    }
    return barra
        .orden(consulta.antiguas() ? Listado.ANTIGUAS : "", Listado.POR_FECHA)
        .sacar("/donante-donaciones")
        .listo();
  }

  private static String texto(String valor) {
    return valor == null || valor.isBlank() ? null : valor.strip();
  }

  /** El estado del filtro por su tono ("en-camino"); uno desconocido no filtra. */
  private static EstadoEntrega estado(String estado) {
    return Arrays.stream(EstadoEntrega.values())
        .filter(e -> e.tono().equals(estado))
        .findFirst()
        .orElse(null);
  }

  /** La página pedida, desde 1; una mal escrita es la primera. */
  private static int pagina(String pagina) {
    try {
      return pagina == null ? 1 : Math.max(1, Integer.parseInt(pagina.strip()));
    } catch (NumberFormatException malEscrita) {
      return 1;
    }
  }

  private static Optional<YearMonth> mes(String mes) {
    if (mes == null || mes.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(YearMonth.parse(mes.strip()));
    } catch (DateTimeParseException malEscrito) {
      return Optional.empty();
    }
  }

  @GetMapping("/donante-donaciones-detalle")
  String detalle(
      @RequestParam(required = false) String donacion, Model model, HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    String vista =
        pantallas.mostrarDonante("donante-donaciones-detalle", donante.get(), model, pedido);
    model.addAttribute("detalle", donaciones.detalle(donante.get(), donacion).orElse(null));
    return vista;
  }
}
