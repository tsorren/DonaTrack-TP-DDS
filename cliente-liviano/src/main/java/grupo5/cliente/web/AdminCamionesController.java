package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.Listado;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Camiones del admin (RF-18) con los datos del puerto; el atributo se llama {@code flota} y la
 * barra, {@code listado}. Las pantallas de resultado ({@code -agregado}, {@code -deshabilitado},
 * {@code -habilitado} y {@code -agregar-error}) son de la maqueta: fuera de ella el envío vuelve a
 * la lista con el aviso ({@link AdminCamionesEnvios}) y esas direcciones no existen.
 */
@Controller
public class AdminCamionesController {

  private static final String PANTALLA = "admin-camiones";

  /** Las pestañas de estado ('' es Todos). */
  private static final List<String> ESTADOS = List.of("ruta", "disponible", "deshabilitado");

  private static final List<Listado.Opcion> CAPACIDADES =
      List.of(
          new Listado.Opcion("", "Todas"),
          new Listado.Opcion("chico", "Hasta 1.500 kg"),
          new Listado.Opcion("mediano", "De 1.500 a 3.000 kg"),
          new Listado.Opcion("grande", "Más de 3.000 kg"));

  private final AdminOperacionPuerto operacion;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminCamionesController(
      AdminOperacionPuerto operacion, Pantallas pantallas, Environment entorno) {
    this.operacion = operacion;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping({
    "/admin-camiones",
    "/admin-camiones-agregado",
    "/admin-camiones-deshabilitado",
    "/admin-camiones-habilitado",
    "/admin-camiones-agregar-error"
  })
  String camiones(Model model, HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    String accion = pantalla.substring(PANTALLA.length()).replaceFirst("^-", "");
    if (!accion.isEmpty() && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String vista = pantallas.mostrar(pantalla, model);
    model.addAttribute(
        "flota", operacion.camiones("agregar-error".equals(accion) ? "error" : accion));
    // El error del alta filtra sobre la lista; los resultados, sobre su pantalla
    boolean enLaLista = accion.isEmpty() || "agregar-error".equals(accion);
    model.addAttribute("listado", listado(enLaLista ? "/" + PANTALLA : "/" + pantalla, pedido));
    return vista;
  }

  /**
   * La barra (R11-B04): la búsqueda por patente o nombre y la capacidad. La pestaña de estado y el
   * camión elegido se conservan ocultos; "Sacar los filtros" lleva a {@code base} sin nada, como
   * antes. La plantilla filtra sobre la flota.
   */
  private static Listado listado(String base, HttpServletRequest pedido) {
    String estado = pedido.getParameter("estado");
    String buscar = pedido.getParameter("buscar");
    return Listado.en("camiones", base, "Filtrar los camiones")
        .oculto("estado", estado != null && ESTADOS.contains(estado) ? estado : "")
        .oculto("camion", pedido.getParameter("camion"))
        .buscar(
            "Buscar por patente o nombre",
            buscar == null ? "" : buscar.strip(),
            "Por ejemplo: AB 123 CD o Norte")
        .otra(
            new Listado.Eleccion(
                "capacidad", "Capacidad", pedido.getParameter("capacidad"), CAPACIDADES))
        .sacar(base)
        .listo();
  }
}
