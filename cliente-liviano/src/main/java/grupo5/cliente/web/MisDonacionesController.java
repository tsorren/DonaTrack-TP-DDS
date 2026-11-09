package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
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
 * encontramos esa donación".
 */
@Controller
public class MisDonacionesController {

  private static final String INGRESAR = "redirect:/publico-ingresar";

  private final DonacionesPuerto donaciones;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  MisDonacionesController(DonacionesPuerto donaciones, CuentaActual cuentas, Pantallas pantallas) {
    this.donaciones = donaciones;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-donaciones")
  String misDonaciones(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("donante-donaciones", model);
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    model.addAttribute("misDonaciones", donaciones.misDonaciones(donante.get()));
    return vista;
  }

  @GetMapping("/donante-donaciones-detalle")
  String detalle(
      @RequestParam(required = false) String donacion, Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("donante-donaciones-detalle", model);
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    model.addAttribute("detalle", donaciones.detalle(donante.get(), donacion).orElse(null));
    return vista;
  }
}
