package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entidades del donante (RF-06) con la página que pide la dirección ({@code ?nombre=} y {@code
 * ?pagina=}; una página mal escrita es la 1), en la versión del tipo de la cuenta. El atributo es
 * {@code misEntidades}. {@code depositoDeEjemplo} es true solo en la maqueta: con datos reales
 * todavía no hay de dónde sacar el depósito (R11-09) y "Cómo llega tu donación" va sin dirección.
 * Sin cuenta, manda a ingresar.
 */
@Controller
public class DonanteEntidadesController {

  private final DonanteEntidadesPuerto entidades;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;
  private final boolean maqueta;

  DonanteEntidadesController(
      DonanteEntidadesPuerto entidades,
      CuentaActual cuentas,
      Pantallas pantallas,
      Environment entorno) {
    this.entidades = entidades;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping("/donante-entidades")
  String entidades(
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String pagina,
      Model model,
      HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    String vista = pantallas.mostrarDonante("donante-entidades", donante.get(), model, pedido);
    int numero = pagina != null && pagina.matches("[1-9]\\d{0,3}") ? Integer.parseInt(pagina) : 1;
    model.addAttribute("depositoDeEjemplo", maqueta);
    model.addAttribute(
        "misEntidades",
        entidades.entidades(donante.get(), nombre == null ? "" : nombre.strip(), numero));
    return vista;
  }
}
