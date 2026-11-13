package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.ConsultaImportaciones;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Las importaciones de donantes del admin (R11-20, ficha r11-p1-importaciones): en curso e
 * históricas, de a una página, con la búsqueda, los filtros y el orden en la dirección y resueltos
 * en la fuente. Es una pestaña de la sección Donantes. Un estado o una fecha que no se entiende se
 * descarta y se avisa arriba de la lista. Con HTMX ({@code hx-boost}) se pide la misma página: el
 * layout toma el {@code main}.
 */
@Controller
public class AdminImportacionesController {

  static final String PANTALLA = "admin-importaciones";

  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

  private final AdminPersonasPuerto personas;
  private final Pantallas pantallas;
  private final Clock reloj;

  AdminImportacionesController(AdminPersonasPuerto personas, Pantallas pantallas, Clock reloj) {
    this.personas = personas;
    this.pantallas = pantallas;
    this.reloj = reloj;
  }

  @GetMapping("/" + PANTALLA)
  String importaciones(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String subida,
      @RequestParam(required = false) String desde,
      @RequestParam(required = false) String hasta,
      @RequestParam(required = false) String orden,
      @RequestParam(required = false) String escenario,
      HttpServletRequest pedido,
      Model model) {
    String vista = pantallas.mostrar(PANTALLA, model);
    ConsultaImportaciones consulta =
        ConsultaImportaciones.de(
            buscar,
            estado,
            subida,
            desde,
            hasta,
            orden,
            Paginadores.pedida(pedido),
            LocalDate.now(reloj.withZone(ARGENTINA)));
    AdminImportacionesVista datos = personas.importaciones(consulta, escenario);
    model.addAttribute("adminImportaciones", datos);
    model.addAttribute(
        "actualizar",
        pedido.getQueryString() == null
            ? pedido.getRequestURI()
            : pedido.getRequestURI() + "?" + pedido.getQueryString());
    model.addAttribute(
        "paginador", Paginadores.de(pedido, "Páginas de importaciones", datos.importaciones()));
    return vista;
  }
}
