package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Confirmar recepción (RF-12) y las pantallas que usan su fragmento de entregas: el acta con
 * errores, Llegó con un problema y Recepción confirmada (las {@code -sellada-<clave>} son las
 * respuestas de la maqueta). Sin entidad, manda a ingresar.
 */
@Controller
public class EntidadConfirmarController {

  private final EntidadConfirmarPuerto confirmar;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadConfirmarController(
      EntidadConfirmarPuerto confirmar, CuentaActual cuentas, Pantallas pantallas) {
    this.confirmar = confirmar;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping({
    "/entidad-seguimiento-confirmar",
    "/entidad-seguimiento-confirmar-error",
    "/entidad-seguimiento-problema",
    "/entidad-seguimiento-sellada",
    "/entidad-seguimiento-sellada-lentejas",
    "/entidad-seguimiento-sellada-ropa"
  })
  String acta(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar(Entidad.pantalla(pedido), model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    model.addAttribute("entregasPorConfirmar", confirmar.entregas(entidad.get()));
    model.addAttribute("hoyActa", confirmar.hoy());
    return vista;
  }
}
