package grupo5.cliente.web;

import grupo5.cliente.puertos.AreaDeLaSesion;
import grupo5.cliente.puertos.SolicitudEntidadPuerto;
import grupo5.cliente.puertos.SolicitudEntidadPuerto.YaAprobada;
import grupo5.cliente.seguridad.UsuarioActual;
import grupo5.cliente.vistas.SolicitudEntidadVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * "Recibimos la solicitud" y "Corregí los datos de tu organización" (docs/interfaz/flujos/
 * r11-p2-pendiente-solicitud.md, R11-41). Con la cuenta cuya solicitud espera la aprobación, la
 * primera muestra lo que envió (lo lee de Donaciones) y la segunda, el formulario lleno; sin
 * sesión, la solicitud sale sin datos. Si Donaciones no contesta, un aviso en lugar de los datos;
 * si el equipo ya la aprobó, el aviso de "Ingresar de nuevo". Guardar lo hace {@link
 * AccesoController} (fuera de la maqueta, donde el formulario vuelve a su pantalla).
 */
@Controller
public class SolicitudEntidadController {

  static final String ENVIADA = "publico-registro-entidad-enviada";
  static final String CORREGIR = "publico-registro-entidad-corregir";

  private static final Logger LOG = LoggerFactory.getLogger(SolicitudEntidadController.class);

  private final Pantallas pantallas;
  private final SolicitudEntidadPuerto solicitudes;
  private final AreaDeLaSesion sesiones;

  SolicitudEntidadController(
      Pantallas pantallas, SolicitudEntidadPuerto solicitudes, AreaDeLaSesion sesiones) {
    this.pantallas = pantallas;
    this.solicitudes = solicitudes;
    this.sesiones = sesiones;
  }

  @GetMapping("/" + ENVIADA)
  String solicitud(
      @AuthenticationPrincipal OidcUser usuario, HttpServletRequest pedido, Model model) {
    boolean pendiente = sesiones.pendiente(pedido).isPresent();
    if (usuario != null || pendiente) {
      model.addAttribute("sesionPendiente", true);
    }
    if (pendiente) {
      model.addAttribute("enLaSolicitud", true);
      try {
        model.addAttribute("solicitud", solicitudes.leer(personaId(usuario)));
      } catch (YaAprobada aprobada) {
        model.addAttribute("aprobada", true);
      } catch (RuntimeException falla) {
        LOG.warn("No se pudo leer la solicitud de la entidad", falla);
        model.addAttribute("sinDatos", true);
      }
    }
    return pantallas.mostrar(ENVIADA, model);
  }

  @GetMapping("/" + CORREGIR)
  String corregir(
      @AuthenticationPrincipal OidcUser usuario, HttpServletRequest pedido, Model model) {
    if (sesiones.pendiente(pedido).isEmpty()) {
      return usuario == null ? "redirect:/publico-ingresar" : "redirect:/inicio";
    }
    try {
      SolicitudEntidadVista datos = solicitudes.leer(personaId(usuario));
      return formulario(pantallas, model, datos, List.of(), false);
    } catch (YaAprobada aprobada) {
      return "redirect:/" + ENVIADA;
    } catch (RuntimeException falla) {
      LOG.warn("No se pudo leer la solicitud de la entidad para corregirla", falla);
      model.addAttribute("sinDatos", true);
      model.addAttribute("sesionPendiente", true);
      return pantallas.mostrar(CORREGIR, model);
    }
  }

  /**
   * El formulario de Corregir con {@code datos} (lo guardado o lo escrito), los campos que
   * Donaciones rechazó y si no se pudo guardar.
   */
  static String formulario(
      Pantallas pantallas,
      Model model,
      SolicitudEntidadVista datos,
      List<String> campos,
      boolean fallo) {
    model.addAttribute("datos", datos);
    model.addAttribute("campos", campos);
    model.addAttribute("fallo", fallo);
    model.addAttribute("sesionPendiente", true);
    return pantallas.mostrar(CORREGIR, model);
  }

  /** La persona de la cuenta; en la maqueta, sin token, no hay y el adaptador no la usa. */
  private static UUID personaId(OidcUser usuario) {
    return usuario == null ? null : UsuarioActual.desde(usuario.getClaims()).personaId();
  }
}
