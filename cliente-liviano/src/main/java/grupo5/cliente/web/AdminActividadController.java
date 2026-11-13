package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminActividadPuerto;
import grupo5.cliente.puertos.AdministradorActual;
import grupo5.cliente.vistas.AdminActividadVista;
import grupo5.cliente.vistas.ConsultaActividad;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * El registro de actividad de administración (R11-26, ficha r11-p2-actividad): quién hizo qué y
 * cuándo, de a una página, con la búsqueda por administrador y los filtros en la dirección y
 * resueltos en la fuente. Es la pestaña Actividad de Administradores. Una cuenta, una acción o una
 * fecha que no se entiende se descarta y se avisa arriba de la lista. Con HTMX ({@code hx-boost})
 * se pide la misma página: el layout toma el {@code main}.
 */
@Controller
public class AdminActividadController {

  static final String PANTALLA = "admin-actividad";

  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

  private final AdminActividadPuerto actividad;
  private final AdministradorActual actual;
  private final Pantallas pantallas;
  private final Clock reloj;

  AdminActividadController(
      AdminActividadPuerto actividad,
      AdministradorActual actual,
      Pantallas pantallas,
      Clock reloj) {
    this.actividad = actividad;
    this.actual = actual;
    this.pantallas = pantallas;
    this.reloj = reloj;
  }

  @GetMapping("/" + PANTALLA)
  String actividad(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String autor,
      @RequestParam(required = false) String accion,
      @RequestParam(required = false) String fecha,
      @RequestParam(required = false) String desde,
      @RequestParam(required = false) String hasta,
      @RequestParam(required = false) String escenario,
      HttpServletRequest pedido,
      Model model) {
    String vista = pantallas.mostrar(PANTALLA, model);
    ConsultaActividad consulta =
        ConsultaActividad.de(
            buscar,
            autor,
            accion,
            fecha,
            desde,
            hasta,
            Paginadores.pedida(pedido),
            LocalDate.now(reloj.withZone(ARGENTINA)));
    String propia = actual.cuenta(pedido).orElse(null);
    AdminActividadVista datos = actividad.actividad(consulta, propia, escenario);
    model.addAttribute("adminActividad", datos);
    model.addAttribute(
        "actualizar",
        pedido.getQueryString() == null
            ? pedido.getRequestURI()
            : pedido.getRequestURI() + "?" + pedido.getQueryString());
    model.addAttribute(
        "paginador", Paginadores.de(pedido, "Páginas de actividad", datos.acciones()));
    return vista;
  }
}
