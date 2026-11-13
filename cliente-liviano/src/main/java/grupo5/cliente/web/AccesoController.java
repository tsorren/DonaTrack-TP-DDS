package grupo5.cliente.web;

import static grupo5.cliente.seguridad.EstadoDeCuenta.SIN_PERFIL;
import static grupo5.cliente.seguridad.EstadoDeCuenta.SOLICITUD_RECIBIDA;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import grupo5.cliente.cuentas.Perfil;
import grupo5.cliente.cuentas.Perfil.DonanteEmpresa;
import grupo5.cliente.cuentas.Perfil.DonantePersona;
import grupo5.cliente.cuentas.Perfil.Entidad;
import grupo5.cliente.cuentas.PerfilService;
import grupo5.cliente.puertos.SolicitudEntidadPuerto;
import grupo5.cliente.puertos.SolicitudEntidadPuerto.DatosRechazados;
import grupo5.cliente.puertos.SolicitudEntidadPuerto.YaAprobada;
import grupo5.cliente.seguridad.Area;
import grupo5.cliente.seguridad.UsuarioActual;
import grupo5.cliente.vistas.SolicitudEntidadVista;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Las pantallas de ingreso y registro las muestra Keycloak: quien no tiene sesión va a él. Quien
 * creó su cuenta y todavía no tiene perfil completa sus datos en los formularios de registro; al
 * guardarlos vuelve a ingresar (sin pedir la clave: la sesión de Keycloak sigue abierta) para que
 * el token traiga su persona y sus permisos. {@code /inicio} manda a cada una al inicio de su área.
 */
@Controller
@Profile("!maqueta")
public class AccesoController {

  private static final Logger LOG = LoggerFactory.getLogger(AccesoController.class);
  private static final String INGRESAR = "redirect:/oauth2/authorization/keycloak";
  private static final String DONANTE = "publico-registro-donante";
  private static final String ENTIDAD = "publico-registro-entidad";
  private static final String SOLICITUD = "redirect:/" + SolicitudEntidadController.ENVIADA;

  private final Pantallas pantallas;
  private final PerfilService perfiles;
  private final SolicitudEntidadPuerto solicitudes;

  AccesoController(
      Pantallas pantallas, PerfilService perfiles, SolicitudEntidadPuerto solicitudes) {
    this.pantallas = pantallas;
    this.perfiles = perfiles;
    this.solicitudes = solicitudes;
  }

  @GetMapping("/publico-ingresar")
  String ingresar() {
    return INGRESAR;
  }

  /** Estas pantallas son de la maqueta: Keycloak resuelve el error y la recuperación de clave. */
  @GetMapping({
    "/publico-ingresar-error",
    "/publico-recuperar",
    "/publico-recuperar-enviado",
    "/publico-registro-donante-error",
    "/publico-registro-entidad-error"
  })
  void soloEnLaMaqueta() {
    throw new ResponseStatusException(NOT_FOUND);
  }

  /** El prefijo literal la hace más específica que {@code /{pantalla}} de las pantallas. */
  @GetMapping("/publico-registro{tipo:(?:-donante|-entidad)?}")
  String formulario(
      @AuthenticationPrincipal OidcUser usuario, @PathVariable String tipo, Model model) {
    if (usuario == null) {
      return INGRESAR + "?prompt=create";
    }
    return sinPerfil(usuario)
        ? completarPerfil(model, "publico-registro" + tipo)
        : "redirect:/inicio";
  }

  @PostMapping(path = "/" + DONANTE, params = "tipo=persona")
  String guardarDonantePersona(
      @AuthenticationPrincipal OidcUser usuario,
      @ModelAttribute DonantePersona perfil,
      @RequestParam(required = false) String terminos,
      HttpSession sesion,
      Model model) {
    return guardar(usuario, perfil, terminos, sesion, model, DONANTE);
  }

  @PostMapping("/" + DONANTE)
  String guardarDonanteEmpresa(
      @AuthenticationPrincipal OidcUser usuario,
      @ModelAttribute DonanteEmpresa perfil,
      @RequestParam(required = false) String terminos,
      HttpSession sesion,
      Model model) {
    return guardar(usuario, perfil, terminos, sesion, model, DONANTE);
  }

  @PostMapping("/" + ENTIDAD)
  String guardarEntidad(
      @AuthenticationPrincipal OidcUser usuario,
      @ModelAttribute Entidad perfil,
      @RequestParam(required = false) String terminos,
      HttpSession sesion,
      Model model) {
    return guardar(usuario, perfil, terminos, sesion, model, ENTIDAD);
  }

  /**
   * Guarda lo corregido de la solicitud que espera la aprobación (R11-41) y vuelve a "Recibimos la
   * solicitud" con el aviso. Si Donaciones no acepta los datos o no contesta, el mismo formulario
   * con lo escrito; si el equipo ya la aprobó, la solicitud lo explica. El correo es el de la
   * cuenta: no viaja.
   */
  @PostMapping("/" + SolicitudEntidadController.CORREGIR)
  String corregirSolicitud(
      @AuthenticationPrincipal OidcUser usuario,
      @ModelAttribute Entidad cambios,
      RedirectAttributes despues,
      Model model) {
    UsuarioActual cuenta = UsuarioActual.desde(usuario.getClaims());
    if (cuenta.estado() != SOLICITUD_RECIBIDA) {
      return "redirect:/inicio";
    }
    SolicitudEntidadVista escrito =
        new SolicitudEntidadVista(
            cambios.entidad(),
            cambios.queEs(),
            cambios.tipo(),
            cambios.calle(),
            cambios.altura(),
            cambios.codigoPostal(),
            cambios.localidad(),
            cambios.provincia(),
            cambios.representante(),
            usuario.getEmail());
    try {
      solicitudes.corregir(cuenta.personaId(), escrito);
    } catch (YaAprobada aprobada) {
      return SOLICITUD;
    } catch (DatosRechazados rechazo) {
      // Sin un campo que marcar (un rechazo que el formulario no conoce), se avisa como falla.
      return SolicitudEntidadController.formulario(
          pantallas, model, escrito, rechazo.campos(), rechazo.campos().isEmpty());
    } catch (RuntimeException falla) {
      LOG.error("No se pudo corregir la solicitud de la cuenta {}", usuario.getSubject(), falla);
      return SolicitudEntidadController.formulario(pantallas, model, escrito, List.of(), true);
    }
    despues.addFlashAttribute("guardado", true);
    return SOLICITUD;
  }

  /**
   * "Ingresar de nuevo", cuando el equipo ya aprobó la solicitud: cierra la sesión del sitio y
   * vuelve a ingresar sin pedir la contraseña (la de Keycloak sigue abierta), para que el token
   * traiga el rol nuevo.
   */
  @PostMapping("/" + SolicitudEntidadController.ENVIADA)
  String ingresarDeNuevo(HttpSession sesion) {
    sesion.invalidate();
    return INGRESAR;
  }

  @GetMapping("/inicio")
  String inicio(@AuthenticationPrincipal OidcUser usuario) {
    return "redirect:"
        + UsuarioActual.desde(usuario.getClaims()).area().map(Area::inicio).orElse("/");
  }

  /**
   * Guardar el perfil anota la aceptación de los legales vigentes: sin la casilla marcada (el
   * navegador la pide con {@code required}), no se guarda.
   */
  private String guardar(
      OidcUser usuario,
      Perfil perfil,
      String terminos,
      HttpSession sesion,
      Model model,
      String pantalla) {
    if (!sinPerfil(usuario)) {
      return "redirect:/inicio";
    }
    if (terminos == null) {
      throw new ResponseStatusException(BAD_REQUEST);
    }
    try {
      perfiles.completar(usuario.getSubject(), perfil);
    } catch (RuntimeException falla) {
      LOG.error("No se pudo completar el perfil de la cuenta {}", usuario.getSubject(), falla);
      model.addAttribute("fallo", true);
      return completarPerfil(model, pantalla);
    }
    sesion.invalidate();
    return INGRESAR;
  }

  private String completarPerfil(Model model, String pantalla) {
    model.addAttribute("completarPerfil", true);
    model.addAttribute("sesionPendiente", true);
    return pantallas.mostrar(pantalla, model);
  }

  private static boolean sinPerfil(OidcUser usuario) {
    return UsuarioActual.desde(usuario.getClaims()).estado() == SIN_PERFIL;
  }
}
