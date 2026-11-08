package grupo5.cliente.web;

import static grupo5.cliente.seguridad.EstadoDeCuenta.SIN_PERFIL;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import grupo5.cliente.cuentas.Perfil;
import grupo5.cliente.cuentas.Perfil.DonanteEmpresa;
import grupo5.cliente.cuentas.Perfil.DonantePersona;
import grupo5.cliente.cuentas.Perfil.Entidad;
import grupo5.cliente.cuentas.PerfilService;
import grupo5.cliente.seguridad.Area;
import grupo5.cliente.seguridad.UsuarioActual;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.server.ResponseStatusException;

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

  private final Pantallas pantallas;
  private final PerfilService perfiles;

  AccesoController(Pantallas pantallas, PerfilService perfiles) {
    this.pantallas = pantallas;
    this.perfiles = perfiles;
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
      HttpSession sesion,
      Model model) {
    return guardar(usuario, perfil, sesion, model, DONANTE);
  }

  @PostMapping("/" + DONANTE)
  String guardarDonanteEmpresa(
      @AuthenticationPrincipal OidcUser usuario,
      @ModelAttribute DonanteEmpresa perfil,
      HttpSession sesion,
      Model model) {
    return guardar(usuario, perfil, sesion, model, DONANTE);
  }

  @PostMapping("/" + ENTIDAD)
  String guardarEntidad(
      @AuthenticationPrincipal OidcUser usuario,
      @ModelAttribute Entidad perfil,
      HttpSession sesion,
      Model model) {
    return guardar(usuario, perfil, sesion, model, ENTIDAD);
  }

  @GetMapping("/inicio")
  String inicio(@AuthenticationPrincipal OidcUser usuario) {
    return "redirect:"
        + UsuarioActual.desde(usuario.getClaims()).area().map(Area::inicio).orElse("/");
  }

  private String guardar(
      OidcUser usuario, Perfil perfil, HttpSession sesion, Model model, String pantalla) {
    if (!sinPerfil(usuario)) {
      return "redirect:/inicio";
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
    return pantallas.mostrar(pantalla, model);
  }

  private static boolean sinPerfil(OidcUser usuario) {
    return UsuarioActual.desde(usuario.getClaims()).estado() == SIN_PERFIL;
  }
}
