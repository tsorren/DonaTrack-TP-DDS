package grupo5.cliente.maqueta;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Comparaciones de diseño (skill iteracion-ab). {@code ?variante=<tema>-a|b} suma {@code
 * static/css/variantes/<variante>.css} a la pantalla o la reemplaza por {@code
 * templates/variantes/<variante>.html}.
 */
class VariantesInterceptor implements HandlerInterceptor {

  private static final String ATRIBUTO = VariantesInterceptor.class.getName();
  private static final String ESTILOS = "classpath*:static/css/variantes/*.css";
  private static final String ESTRUCTURAS = "classpath*:templates/variantes/*.html";

  private final ResourcePatternResolver recursos;

  VariantesInterceptor(ResourcePatternResolver recursos) {
    this.recursos = recursos;
  }

  private record Variante(String nombre, boolean estilo, boolean estructura) {}

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String pedida = request.getParameter("variante");
    if (pedida == null) {
      return true;
    }
    Set<String> estilos = nombres(ESTILOS);
    Set<String> estructuras = nombres(ESTRUCTURAS);
    String nombre = existente(pedida, Stream.concat(estilos.stream(), estructuras.stream()));
    if (nombre == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe esa variante");
      return false;
    }
    request.setAttribute(
        ATRIBUTO, new Variante(nombre, estilos.contains(nombre), estructuras.contains(nombre)));
    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    if (modelAndView == null || !(request.getAttribute(ATRIBUTO) instanceof Variante variante)) {
      return;
    }
    String vista = modelAndView.getViewName();
    boolean pantalla = vista != null && !vista.startsWith("redirect:");
    if (variante.estructura() && pantalla) {
      modelAndView.setViewName("variantes/" + variante.nombre());
    }
    if (variante.estilo()) {
      modelAndView.addObject("varianteCss", variante.nombre());
    }
  }

  /**
   * El nombre que arma la ruta y la vista sale de los archivos que existen, nunca del parámetro.
   */
  private static String existente(String pedido, Stream<String> nombres) {
    return nombres.filter(pedido::equals).findFirst().orElse(null);
  }

  /** Nombres de archivo sin extensión. */
  private Set<String> nombres(String patron) throws IOException {
    return Arrays.stream(recursos.getResources(patron))
        .map(Resource::getFilename)
        .filter(Objects::nonNull)
        .map(archivo -> archivo.substring(0, archivo.lastIndexOf('.')))
        .collect(Collectors.toSet());
  }
}
