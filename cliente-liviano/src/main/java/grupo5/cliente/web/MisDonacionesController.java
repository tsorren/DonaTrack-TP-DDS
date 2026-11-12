package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
 * persona), solo las registradas ese mes; un mes mal escrito se ignora.
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
      @RequestParam(required = false) String mes, Model model, HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    String vista = pantallas.mostrarDonante("donante-donaciones", donante.get(), model, pedido);
    Optional<YearMonth> delMes = mes(mes);
    model.addAttribute(
        "misDonaciones",
        delMes.isPresent()
            ? donaciones.misDonaciones(donante.get(), delMes.get())
            : donaciones.misDonaciones(donante.get()));
    model.addAttribute("mesClave", delMes.map(YearMonth::toString).orElse(null));
    model.addAttribute("mesTexto", delMes.map(MES::format).orElse(null));
    return vista;
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
