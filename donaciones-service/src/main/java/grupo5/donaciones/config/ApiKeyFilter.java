package grupo5.donaciones.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege con una API key las rutas de administración del broker de logística ({@code
 * /api/logistica/proveedores} y {@code /api/logistica/proveedor-preferido}). La clave está en
 * {@code donatrack.logistica.admin-api-key}.
 *
 * <p>Falla cerrado: sin clave configurada, o con una clave ausente o incorrecta, responde 401 y la
 * petición no llega al controller. Cualquier otra ruta bajo {@code /api/logistica/proveedor}
 * también se rechaza (por ejemplo la del callback HTTP de proveedores, que ya no existe: los
 * proveedores informan por mensajería). La comparación no depende del contenido de la clave y las
 * claves nunca se loguean. Es una protección mínima hasta el {@code auth-service} de la Entrega 6;
 * no usa Spring Security (no está en el proyecto).
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

  static final String HEADER_API_KEY = "X-API-Key";
  static final String PREFIJO_PROTEGIDO = "/api/logistica/proveedor";
  static final String PROPIEDAD_CLAVE_ADMIN = "donatrack.logistica.admin-api-key";
  static final String CODIGO_NO_AUTORIZADO = "ERR-AUT-401";

  private static final Pattern RUTA_ADMIN =
      Pattern.compile("^/api/logistica/(proveedores|proveedor-preferido)/?$");

  private final Environment environment;

  public ApiKeyFilter(Environment environment) {
    this.environment = environment;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String ruta = rutaNormalizada(request);
    return ruta != null && !ruta.startsWith(PREFIJO_PROTEGIDO);
  }

  /**
   * La ruta sin parámetros de ruta ({@code ;...}), {@code .}/{@code ..} ni barras repetidas: al
   * menos tan estricta como la que usa Spring para elegir el handler. Comparar contra la URI cruda
   * se podría esquivar con {@code /api;x=1/logistica/...}, {@code //api/...} o {@code
   * /api/x/../logistica/...}. Devuelve {@code null} si la URI no es interpretable: en ese caso se
   * filtra (falla cerrado).
   */
  private static String rutaNormalizada(HttpServletRequest request) {
    try {
      // Spring quita los ";..." de cada segmento al elegir el handler; se quitan igual acá.
      // Las barras se colapsan antes de parsear: URI.create("//api/x") tomaría "api" por un host.
      String sinBarrasRepetidas =
          request.getRequestURI().replaceAll(";[^/]*", "").replaceAll("/{2,}", "/");
      return URI.create(sinBarrasRepetidas).normalize().getPath();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String rutaNormalizada = rutaNormalizada(request);
    String ruta = rutaNormalizada == null ? "" : rutaNormalizada;
    if (!RUTA_ADMIN.matcher(ruta).matches()) {
      log.warn("[API-KEY] Ruta no reconocida bajo {}: se rechaza", PREFIJO_PROTEGIDO);
      rechazar(response);
      return;
    }
    String esperada = environment.getProperty(PROPIEDAD_CLAVE_ADMIN);
    if (esperada == null || esperada.isBlank()) {
      log.warn("[API-KEY] Falta configurar {}: se rechaza", PROPIEDAD_CLAVE_ADMIN);
      rechazar(response);
      return;
    }
    String recibida = request.getHeader(HEADER_API_KEY);
    if (recibida == null || !ClaveSegura.coinciden(recibida, esperada)) {
      log.warn("[API-KEY] Clave de administración ausente o incorrecta");
      rechazar(response);
      return;
    }
    chain.doFilter(request, response);
  }

  private static void rechazar(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"code\":\""
                + CODIGO_NO_AUTORIZADO
                + "\",\"type\":\"NoAutorizado\",\"details\":\"API key ausente o inválida\","
                + "\"timestamp\":\""
                + LocalDateTime.now(ZoneOffset.UTC)
                + "\"}");
  }
}
