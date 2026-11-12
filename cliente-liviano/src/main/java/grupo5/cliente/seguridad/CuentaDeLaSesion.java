package grupo5.cliente.seguridad;

import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.EntidadPerfilPuerto;
import grupo5.cliente.vistas.CuentaVista;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * La cuenta del menú y del saludo con el ingreso real (R11-44): el nombre y apellido de la persona,
 * la razón social de la empresa o de la organización, o el nombre de quien administra; nunca el
 * rol. El ID token no trae nombres (RNF-AUTH-09): se piden una vez por sesión (a Donaciones o, para
 * la administración, al proveedor de ingreso) y quedan guardados. Si el pedido falla, el menú dice
 * "Tu cuenta" y la próxima pantalla vuelve a probar. El renglón de abajo dice qué es la cuenta:
 * "Persona donante", "Organización", "Administración".
 */
final class CuentaDeLaSesion implements CuentasDelMenu {

  /** El nombre cuando no se sabe: nunca el rol. */
  static final String SIN_NOMBRE = "Tu cuenta";

  private static final String NOMBRE = "cuenta.nombre";
  private static final String ROL = "cuenta.rol";
  private static final String DE_QUIEN = "cuenta.de";

  private final String cambiarClave;
  private final ObjectProvider<DonantePerfilPuerto> donantes;
  private final ObjectProvider<EntidadPerfilPuerto> entidades;
  private final ObjectProvider<DirectorioDeAdministradores> administradores;

  CuentaDeLaSesion(
      String emisor,
      ObjectProvider<DonantePerfilPuerto> donantes,
      ObjectProvider<EntidadPerfilPuerto> entidades,
      ObjectProvider<DirectorioDeAdministradores> administradores) {
    this.cambiarClave = emisor + "/account";
    this.donantes = donantes;
    this.entidades = entidades;
    this.administradores = administradores;
  }

  @Override
  public Optional<CuentaVista> de(String prefijoDeArea) {
    return Area.deRuta(prefijoDeArea).map(area -> cuenta(area, true));
  }

  @Override
  public Optional<CuentaVista> deLaSesion(String prefijoDeArea) {
    return Area.deRuta(prefijoDeArea).map(area -> cuenta(area, false));
  }

  @Override
  public void olvidar(HttpServletRequest pedido) {
    HttpSession sesion = pedido.getSession(false);
    if (sesion != null) {
      sesion.removeAttribute(DE_QUIEN);
    }
  }

  private CuentaVista cuenta(Area area, boolean puedePedir) {
    String quien = quien(area);
    HttpSession sesion = sesion();
    if (quien != null
        && sesion != null
        && quien.equals(sesion.getAttribute(DE_QUIEN))
        && sesion.getAttribute(NOMBRE) instanceof String nombre
        && sesion.getAttribute(ROL) instanceof String rol) {
      return new CuentaVista(nombre, rol, cambiarClave);
    }
    CuentaVista pedida = quien == null || !puedePedir ? null : pedir(area);
    if (pedida == null) {
      return new CuentaVista(SIN_NOMBRE, area.etiqueta(), cambiarClave);
    }
    if (sesion != null) {
      sesion.setAttribute(NOMBRE, pedida.nombre());
      sesion.setAttribute(ROL, pedida.rol());
      sesion.setAttribute(DE_QUIEN, quien);
    }
    return new CuentaVista(pedida.nombre(), pedida.rol(), cambiarClave);
  }

  /** Quién es la cuenta del área: el área y su persona (o su {@code sub}, en la administración). */
  private static String quien(Area area) {
    OidcUser usuario = usuario();
    if (usuario == null) {
      return null;
    }
    if (area == Area.ADMIN) {
      return area.prefijo() + ":" + usuario.getSubject();
    }
    UUID persona = UsuarioActual.desde(usuario.getClaims()).personaId();
    return persona == null ? null : area.prefijo() + ":" + persona;
  }

  /**
   * La cuenta según su servicio; null si no hay a quién preguntar, no responde o no tiene nombre.
   */
  private CuentaVista pedir(Area area) {
    try {
      CuentaVista cuenta =
          switch (area) {
            case DONANTE -> {
              DonantePerfilPuerto puerto = donantes.getIfAvailable();
              yield puerto == null ? null : puerto.cuenta(personaId());
            }
            case ENTIDAD -> {
              EntidadPerfilPuerto puerto = entidades.getIfAvailable();
              yield puerto == null ? null : puerto.cuenta(personaId());
            }
            case ADMIN -> administracion();
          };
      return cuenta == null || cuenta.nombre() == null || cuenta.nombre().isBlank() ? null : cuenta;
    } catch (RuntimeException noRespondio) {
      // Sin el nombre, el menú dice "Tu cuenta": la pantalla no falla por el saludo.
      return null;
    }
  }

  /** El nombre de quien administra o, si todavía no lo cargó, su correo (R11-10). */
  private CuentaVista administracion() {
    DirectorioDeAdministradores directorio = administradores.getIfAvailable();
    OidcUser usuario = usuario();
    if (directorio == null || usuario == null) {
      return null;
    }
    return directorio
        .cuenta(usuario.getSubject())
        .map(a -> Objects.requireNonNullElse(a.nombreCompleto(), correo(a)))
        .map(nombre -> new CuentaVista(nombre, Area.ADMIN.etiqueta()))
        .orElse(null);
  }

  private static String correo(Administrador administrador) {
    return administrador.correo() == null ? "" : administrador.correo();
  }

  private static UUID personaId() {
    OidcUser usuario = usuario();
    return usuario == null ? null : UsuarioActual.desde(usuario.getClaims()).personaId();
  }

  private static OidcUser usuario() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .filter(OidcUser.class::isInstance)
        .map(OidcUser.class::cast)
        .orElse(null);
  }

  private static HttpSession sesion() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido
        ? pedido.getRequest().getSession()
        : null;
  }
}
