package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.puertos.DonacionesPuerto.Consulta;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
      Model model,
      HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    String vista = pantallas.mostrarDonante("donante-donaciones", donante.get(), model, pedido);
    Optional<YearMonth> delMes = mes(mes);
    MisDonacionesVista lista =
        donaciones.misDonaciones(
            donante.get(),
            new Consulta(
                delMes.orElse(null),
                estado(estado),
                texto(categoria),
                texto(subcategoria),
                pagina(pagina)));
    model.addAttribute("misDonaciones", lista);
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
