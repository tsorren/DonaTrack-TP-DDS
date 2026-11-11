package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Seguir entregas de la entidad (RF-14): lo asignado (el puerto de Donaciones recibidas) y la
 * última noticia de cada entrega. Sin entidad, manda a ingresar.
 */
@Controller
public class EntidadSeguimientoController {

  private final RecibidasPuerto recibidas;
  private final EntidadSeguimientoPuerto seguimientos;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadSeguimientoController(
      RecibidasPuerto recibidas,
      EntidadSeguimientoPuerto seguimientos,
      CuentaActual cuentas,
      Pantallas pantallas) {
    this.recibidas = recibidas;
    this.seguimientos = seguimientos;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/entidad-seguimiento")
  String seguimiento(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("entidad-seguimiento", model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    model.addAttribute("recibidas", recibidas.recibidas(entidad.get()));
    model.addAttribute("noticiasDeEntregas", seguimientos.noticias(entidad.get()));
    model.addAttribute("dibujoDeEjemplo", seguimientos.dibujoDeEjemplo());
    return vista;
  }
}
