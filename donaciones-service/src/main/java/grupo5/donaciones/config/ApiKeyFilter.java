package grupo5.donaciones.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege con una API key los endpoints que expone el broker de logística. Hoy cubre el callback de
 * los proveedores HTTP ({@code /api/logistica/proveedores/{proveedorId}/avisos}): cada proveedor
 * tiene su propia clave en {@code donatrack.logistica.proveedor.<id>.callback-api-key}, que además
 * le da identidad al callback (la clave de un proveedor no sirve para avisar como otro).
 *
 * <p>Falla cerrado: sin clave configurada para ese proveedor, o con una clave ausente o incorrecta,
 * responde 401 y la petición no llega al controller. La comparación no depende del contenido de la
 * clave y las claves nunca se loguean. Es una protección mínima hasta el {@code auth-service} de la
 * Entrega 6; no usa Spring Security (no está en el proyecto).
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

  static final String HEADER_API_KEY = "X-API-Key";
  static final String PREFIJO_PROVEEDORES = "/api/logistica/proveedores/";
  static final String CODIGO_NO_AUTORIZADO = "ERR-AUT-401";

  private static final Pattern RUTA_AVISOS =
      Pattern.compile("^/api/logistica/proveedores/([A-Za-z0-9_-]+)/avisos/?$");

  private final Environment environment;

  public ApiKeyFilter(Environment environment) {
    this.environment = environment;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String ruta = rutaNormalizada(request);
    return ruta != null && !ruta.startsWith(PREFIJO_PROVEEDORES);
  }

  /**
   * La ruta tal como la mapea el servidor: sin {@code .}/{@code ..} ni barras repetidas. Comparar
   * contra la URI cruda se podría esquivar con {@code //api/...} o {@code /api/x/../logistica/...}.
   * Devuelve {@code null} si la URI no es interpretable: en ese caso se filtra (falla cerrado).
   */
  private static String rutaNormalizada(HttpServletRequest request) {
    try {
      // Las barras se colapsan antes de parsear: URI.create("//api/x") tomaría "api" por un host.
      String sinBarrasRepetidas = request.getRequestURI().replaceAll("/{2,}", "/");
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
    Matcher ruta = RUTA_AVISOS.matcher(rutaNormalizada == null ? "" : rutaNormalizada);
    if (!ruta.matches()) {
      log.warn("[API-KEY] Ruta no reconocida bajo {}: se rechaza", PREFIJO_PROVEEDORES);
      rechazar(response);
      return;
    }
    String proveedorId = ruta.group(1);
    String esperada =
        environment.getProperty(
            "donatrack.logistica.proveedor." + proveedorId + ".callback-api-key");
    if (esperada == null || esperada.isBlank()) {
      log.warn(
          "[API-KEY] El proveedor {} no tiene callback-api-key configurada: se rechaza",
          proveedorId);
      rechazar(response);
      return;
    }
    String recibida = request.getHeader(HEADER_API_KEY);
    if (recibida == null || !coinciden(recibida, esperada)) {
      log.warn("[API-KEY] Clave ausente o incorrecta para el proveedor {}", proveedorId);
      rechazar(response);
      return;
    }
    chain.doFilter(request, response);
  }

  /** Compara los hashes de las claves, así el tiempo no depende de cuánto coincidan. */
  private static boolean coinciden(String recibida, String esperada) {
    return MessageDigest.isEqual(sha256(recibida), sha256(esperada));
  }

  private static byte[] sha256(String valor) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
    }
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
