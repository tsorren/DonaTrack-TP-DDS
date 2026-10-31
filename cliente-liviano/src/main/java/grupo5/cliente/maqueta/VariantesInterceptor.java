package grupo5.cliente.maqueta;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Comparaciones de diseño. {@code ?alternativa=<nombre>} cambia la identidad visual: la página usa
 * {@code static/css/alternativas/<nombre>/tokens.css} y {@code app.css} en lugar de las de
 * Glassmorphism, y su propio juego de plantillas, {@code templates/alternativas/<nombre>/} (la
 * vista {@code paginas/x} pasa a {@code alternativas/<nombre>/paginas/x} si existe; bitácora,
 * 2026-10-30). {@code ?variante=<tema>-a|b} (skill iteracion-ab) suma {@code
 * static/css/variantes/<variante>.css} a la página o la reemplaza por {@code
 * templates/variantes/<variante>.html}, también a una pantalla del prototipo ({@code
 * /prototipo/<pantalla>}, para comparar conceptos de una corrida creativa). Las dos se pueden
 * combinar; una variante de estructura gana sobre las plantillas de la alternativa.
 */
class VariantesInterceptor implements HandlerInterceptor {

  private static final String ATRIBUTO = VariantesInterceptor.class.getName();
  private static final String ATRIBUTO_ALTERNATIVA = ATRIBUTO + ".alternativa";
  private static final String ESTILOS = "classpath*:static/css/variantes/*.css";
  private static final String ESTRUCTURAS = "classpath*:templates/variantes/*.html";
  private static final String ALTERNATIVAS = "classpath*:static/css/alternativas/*/tokens.css";

  private final ResourcePatternResolver recursos;

  VariantesInterceptor(ResourcePatternResolver recursos) {
    this.recursos = recursos;
  }

  private record Variante(String nombre, boolean estilo, boolean estructura) {}

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String alternativaPedida = request.getParameter("alternativa");
    if (alternativaPedida != null) {
      String alternativa = existente(alternativaPedida, alternativas().stream());
      if (alternativa == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe esa alternativa");
        return false;
      }
      request.setAttribute(ATRIBUTO_ALTERNATIVA, alternativa);
    }
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
    if (modelAndView == null) {
      return;
    }
    String vista = modelAndView.getViewName();
    boolean pagina = vista != null && vista.startsWith("paginas/");
    if (request.getAttribute(ATRIBUTO_ALTERNATIVA) instanceof String alternativa) {
      modelAndView.addObject("alternativa", alternativa);
      String propia = "alternativas/" + alternativa + "/" + vista;
      if (pagina && recursos.getResource("classpath:templates/" + propia + ".html").exists()) {
        modelAndView.setViewName(propia);
      }
    }
    if (!(request.getAttribute(ATRIBUTO) instanceof Variante variante)) {
      return;
    }
    boolean prototipo = vista != null && vista.startsWith("prototipo/");
    if (variante.estructura() && (pagina || prototipo)) {
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

  /** Carpetas de static/css/alternativas/ que tienen su tokens.css. */
  private Set<String> alternativas() throws IOException {
    Set<String> carpetas = new HashSet<>();
    for (Resource hoja : recursos.getResources(ALTERNATIVAS)) {
      String[] partes = hoja.getURL().getPath().split("/");
      carpetas.add(partes[partes.length - 2]);
    }
    return carpetas;
  }
}
