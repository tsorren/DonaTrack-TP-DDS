package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entidades del donante (RF-06) con la página que pide la dirección ({@code ?nombre=} y {@code
 * ?pagina=}; una página mal escrita es la 1). El atributo es {@code misEntidades}. Sin cuenta,
 * manda a ingresar.
 */
@Controller
public class DonanteEntidadesController {

  private final DonanteEntidadesPuerto entidades;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  DonanteEntidadesController(
      DonanteEntidadesPuerto entidades, CuentaActual cuentas, Pantallas pantallas) {
    this.entidades = entidades;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-entidades")
  String entidades(
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String pagina,
      Model model,
      HttpServletRequest pedido) {
    String vista = pantallas.mostrar("donante-entidades", model);
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    int numero = pagina != null && pagina.matches("[1-9]\\d{0,3}") ? Integer.parseInt(pagina) : 1;
    model.addAttribute(
        "misEntidades",
        entidades.entidades(donante.get(), nombre == null ? "" : nombre.strip(), numero));
    return vista;
  }
}
