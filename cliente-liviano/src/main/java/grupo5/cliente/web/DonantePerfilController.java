package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Mi perfil del donante con los datos del puerto; también la pantalla de después de guardar de la
 * maqueta, que usa la misma hoja. La persona (y la cuenta sin tipo) ve la de templates/persona/,
 * sin Razón social ni CUIT. El atributo es {@code miPerfil}: {@code perfil} ya es el enlace del
 * layout. Sin cuenta, manda a ingresar.
 */
@Controller
public class DonantePerfilController {

  private static final String GUARDADO = "/donante-perfil-guardado";

  private final DonantePerfilPuerto perfiles;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  DonantePerfilController(DonantePerfilPuerto perfiles, CuentaActual cuentas, Pantallas pantallas) {
    this.perfiles = perfiles;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  // Rutas literales: le ganan a la /{pantalla} de PantallasController.
  @GetMapping({"/donante-perfil", GUARDADO})
  String perfil(Model model, HttpServletRequest pedido) {
    String pantalla =
        pedido.getRequestURI().endsWith(GUARDADO) ? GUARDADO.substring(1) : "donante-perfil";
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    String vista = pantallas.mostrarDonante(pantalla, donante.get(), model, pedido);
    model.addAttribute("miPerfil", perfiles.perfil(donante.get()));
    return vista;
  }
}
