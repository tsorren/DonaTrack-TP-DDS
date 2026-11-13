package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.vistas.AdminEntidadesVista;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Entidades del admin (RF-21 y RF-15) con los datos del puerto, y las pantallas que comparten su
 * hoja después de aprobar, pedir corrección o rechazar (las muestra la maqueta). Reemplaza a la
 * página de solicitudes de antes: las que piden sumarse están en la misma lista.
 */
@Controller
public class AdminEntidadesController {

  private static final String ENTIDADES = "admin-entidades";

  private final AdminPersonasPuerto personas;
  private final Pantallas pantallas;

  AdminEntidadesController(AdminPersonasPuerto personas, Pantallas pantallas) {
    this.personas = personas;
    this.pantallas = pantallas;
  }

  @GetMapping("/" + ENTIDADES)
  String entidades(Model model, HttpServletRequest pedido) {
    return mostrar(ENTIDADES, model, pedido);
  }

  @GetMapping("/" + ENTIDADES + "-{accion:aprobada|correccion|rechazada}")
  String despues(@PathVariable String accion, Model model, HttpServletRequest pedido) {
    return mostrar(ENTIDADES + "-" + accion, model, pedido);
  }

  /**
   * La página pedida ({@code ?pagina=}) con la búsqueda ({@code ?buscar=}); el paginador provisorio
   * (R11-01) solo si la fuente da la lista de a una página y hay más de una.
   */
  private String mostrar(String pantalla, Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar(pantalla, model);
    String buscar = pedido.getParameter("buscar");
    AdminEntidadesVista.Filtro filtro =
        new AdminEntidadesVista.Filtro(
            pedido.getParameter("ver"),
            pedido.getParameter("pedidos"),
            pedido.getParameter("tipo"));
    AdminEntidadesVista datos =
        personas.entidades(
            buscar == null ? "" : buscar.strip(), filtro, Paginadores.pedida(pedido));
    model.addAttribute("adminEntidades", datos);
    model.addAttribute(
        "paginador",
        Paginadores.de(
            pedido,
            "Páginas de organizaciones",
            datos == null || datos.enPaginas() == null ? null : datos.enPaginas().registradas()));
    return vista;
  }
}
