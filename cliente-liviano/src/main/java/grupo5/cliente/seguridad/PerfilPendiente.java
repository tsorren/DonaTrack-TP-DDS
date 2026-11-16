package grupo5.cliente.seguridad;

import grupo5.cliente.web.Pantallas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Una sesión cuya cuenta todavía no completó su perfil, o espera la aprobación de su entidad, solo
 * ve las pantallas de su estado: el resto de las pantallas la lleva ahí. Las hojas, los scripts y
 * el cierre de sesión no son pantallas y pasan.
 */
final class PerfilPendiente extends OncePerRequestFilter {

  private static final Pattern PANTALLA = Pattern.compile("/|/inicio|/" + Pantallas.NOMBRE);

  @Override
  protected void doFilterInternal(
      HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
      throws IOException, ServletException {
    var sesion = SecurityContextHolder.getContext().getAuthentication();
    String ruta = pedido.getRequestURI();
    if (sesion != null
        && PANTALLA.matcher(ruta).matches()
        && sesion.getPrincipal() instanceof OidcUser usuario) {
      EstadoDeCuenta estado = UsuarioActual.desde(usuario.getClaims()).estado();
      if (!estado.permite(ruta)) {
        respuesta.sendRedirect(estado.destino());
        return;
      }
    }
    cadena.doFilter(pedido, respuesta);
  }
}
