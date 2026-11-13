package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.RecibidasVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Seguir entregas de la entidad (RF-14): lo asignado (el puerto de Donaciones recibidas) y la
 * última noticia de cada entrega. Sin entidad, manda a ingresar. Desde R11-43, de a una página de
 * lo que todavía no se recibió, y las noticias solo de las entregas de esa página.
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
    RecibidasVista datos =
        recibidas.recibidas(entidad.get(), RecibidasVista.SIN_RECIBIR, Paginadores.pedida(pedido));
    model.addAttribute("recibidas", datos);
    model.addAttribute("paginador", Paginadores.de(pedido, "Páginas de entregas", datos.tramo()));
    model.addAttribute(
        "noticiasDeEntregas",
        seguimientos.noticias(
            entidad.get(),
            datos.asignadas().stream().map(RecibidasVista.Recibida::clave).toList()));
    model.addAttribute("dibujoDeEjemplo", seguimientos.dibujoDeEjemplo());
    return vista;
  }
}
