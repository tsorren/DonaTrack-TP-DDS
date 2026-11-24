package grupo5.gateway.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Encabezados de seguridad en todas las respuestas del gateway (Q03 de E7). El gateway solo
 * devuelve JSON, así que la política de contenido no permite nada.
 *
 * <p>Corre antes que la cadena de Spring Security para que también el 401 los lleve. Los fija antes
 * de seguir y después ignora cualquier otro valor para esos nombres: el que trae el servicio en una
 * ruta enrutada (los servicios tienen su propia cadena) y el que pone la cadena de {@code
 * common-lib} ({@code nosniff}, {@code DENY} y, solo en {@code /api/**}, su {@code Cache-Control}).
 * Así cada uno sale una sola vez y con este valor. Fuera de {@code /api/**} el {@code
 * Cache-Control} de la cadena pasa tal cual. {@code Strict-Transport-Security} sale también en
 * http, donde el navegador lo ignora; vale cuando haya TLS.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EncabezadosDeSeguridad extends OncePerRequestFilter {

  private static final Map<String, String> SIEMPRE =
      Map.of(
          "X-Content-Type-Options", "nosniff",
          "X-Frame-Options", "DENY",
          "Referrer-Policy", "no-referrer",
          "Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'",
          "Permissions-Policy", "camera=(), microphone=(), geolocation=()",
          "Strict-Transport-Security", "max-age=31536000");

  private static final String CACHE_CONTROL = "Cache-Control";
  private static final String NO_STORE = "no-store";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Map<String, String> fijos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    fijos.putAll(SIEMPRE);
    if (request.getRequestURI().startsWith("/api/")) {
      fijos.put(CACHE_CONTROL, NO_STORE);
    }
    fijos.forEach(response::setHeader);
    chain.doFilter(request, new SinPisarLosFijos(response, fijos));
  }

  /** La respuesta que ve el resto de la cadena: los encabezados fijos no se pisan ni se repiten. */
  private static final class SinPisarLosFijos extends HttpServletResponseWrapper {

    private final Map<String, String> fijos;

    SinPisarLosFijos(HttpServletResponse response, Map<String, String> fijos) {
      super(response);
      this.fijos = fijos;
    }

    @Override
    public void setHeader(String name, String value) {
      if (!fijos.containsKey(name)) {
        super.setHeader(name, value);
      }
    }

    @Override
    public void addHeader(String name, String value) {
      if (!fijos.containsKey(name)) {
        super.addHeader(name, value);
      }
    }
  }
}
