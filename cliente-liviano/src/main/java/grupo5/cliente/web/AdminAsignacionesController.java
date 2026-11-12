package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.ConsultaAsignaciones;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Asignar donaciones del admin (RF-17, R11-24; ficha r11-p1-asignaciones) con los datos del puerto;
 * el atributo se llama {@code asignaciones}. Una pestaña por dirección ({@code
 * ?pestana=propuestas|conflictos|destino}, {@code ?vista=decididas}), con el filtro ({@code
 * ?algoritmo=}, {@code ?categoria=}), la donación elegida ({@code ?donacion=}) y la página ({@code
 * ?pagina=}) en la dirección: anda sin JavaScript y el "atrás" vuelve. Las pantallas de resultado
 * ({@code -aprobada}, {@code -descartada}, {@code -error} y {@code -corrida}) son de la maqueta:
 * fuera de ella el envío vuelve a la lista con el aviso ({@link AdminAsignacionesEnvios}) y esas
 * direcciones no existen. En la maqueta, {@code -corrida?escenario=nuevas} pide la acción {@code
 * nuevas} (las propuestas de hoy).
 */
@Controller
public class AdminAsignacionesController {

  private static final String LISTA = "admin-asignaciones";

  private final AdminOperacionPuerto operacion;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminAsignacionesController(
      AdminOperacionPuerto operacion, Pantallas pantallas, Environment entorno) {
    this.operacion = operacion;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping({
    "/" + LISTA,
    "/admin-asignaciones-aprobada",
    "/admin-asignaciones-descartada",
    "/admin-asignaciones-error",
    "/admin-asignaciones-corrida"
  })
  String asignaciones(
      @RequestParam(required = false) String pestana,
      @RequestParam(required = false) String vista,
      @RequestParam(required = false) String algoritmo,
      @RequestParam(required = false) String categoria,
      @RequestParam(required = false) String donacion,
      @RequestParam(required = false) String escenario,
      Model model,
      HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    if (!LISTA.equals(pantalla) && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String plantilla = pantallas.mostrar(pantalla, model);
    String accion = LISTA.equals(pantalla) ? "" : pantalla.substring(LISTA.length() + 1);
    if ("corrida".equals(accion) && "nuevas".equals(escenario)) {
      accion = "nuevas";
    }
    ConsultaAsignaciones consulta =
        ConsultaAsignaciones.de(
            pestana, vista, algoritmo, categoria, donacion, Paginadores.pedida(pedido));
    AdminAsignacionesVista datos = operacion.asignaciones(consulta, accion);
    model.addAttribute("asignaciones", datos);
    model.addAttribute("paginador", paginador(pedido, datos));
    return plantilla;
  }

  private static Object paginador(HttpServletRequest pedido, AdminAsignacionesVista datos) {
    if (datos.decididas() != null) {
      return Paginadores.de(pedido, "Páginas de propuestas decididas", datos.decididas());
    }
    if (datos.donaciones() != null) {
      return Paginadores.de(pedido, "Páginas de donaciones", datos.donaciones());
    }
    return Paginadores.de(pedido, "Páginas de propuestas", datos.propuestas());
  }
}
