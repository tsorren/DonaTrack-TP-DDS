package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadInicioPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * El Inicio de la entidad: lo asignado (el mismo puerto de Donaciones recibidas) y lo que suma el
 * Inicio (las novedades y las necesidades vigentes). Sin entidad, manda a ingresar.
 */
@Controller
public class EntidadInicioController {

  private final RecibidasPuerto recibidas;
  private final EntidadInicioPuerto inicios;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadInicioController(
      RecibidasPuerto recibidas,
      EntidadInicioPuerto inicios,
      CuentaActual cuentas,
      Pantallas pantallas) {
    this.recibidas = recibidas;
    this.inicios = inicios;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/entidad-inicio")
  String inicio(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("entidad-inicio", model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    model.addAttribute("recibidas", recibidas.recibidas(entidad.get()));
    model.addAttribute("inicio", inicios.inicio(entidad.get()));
    return vista;
  }
}
