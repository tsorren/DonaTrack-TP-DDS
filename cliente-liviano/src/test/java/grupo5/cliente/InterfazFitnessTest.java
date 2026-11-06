package grupo5.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Reglas de la interfaz que se pueden comprobar sin navegador (cliente-liviano/AGENTS.md). Las
 * reglas y las listas permitidas viven solo en este archivo.
 */
class InterfazFitnessTest {

  private static final Path RECURSOS = Path.of("src/main/resources");
  private static final Path CSS = RECURSOS.resolve("static/css");
  private static final String TOKENS = "tokens.css";
  private static final String HTML = ".html";
  private static final Path VARIANTES = CSS.resolve("variantes");
  private static final Path PLANTILLAS = RECURSOS.resolve("templates");

  /** Tokens de uso: lo único que pueden referenciar pantallas y componentes. */
  private static final List<String> PREFIJOS_DE_USO =
      List.of(
          "--color-",
          "--fuente-",
          "--tamano-",
          "--espacio-",
          "--radio-",
          "--borde-",
          "--sombra-",
          "--movimiento-",
          "--capa-",
          "--medida-");

  private static final String PREFIJO_BASE = "--p-";

  /**
   * Pares texto-fondo con contraste AA: 4,5 para texto y 3 para bordes y foco (WCAG 1.4.3 y
   * 1.4.11). Un par nuevo de colores en tokens.css se agrega acá. Rigen para todo tokens.css de
   * static/css/.
   */
  private static final List<Par> PARES =
      List.of(
          new Par("--color-texto", "--color-fondo-pagina", 4.5),
          new Par("--color-texto-suave", "--color-fondo-pagina", 4.5),
          new Par("--color-titulo", "--color-fondo-pagina", 4.5),
          new Par("--color-enlace", "--color-fondo-pagina", 4.5),
          new Par("--color-texto", "--color-fondo", 4.5),
          new Par("--color-texto", "--color-superficie", 4.5),
          new Par("--color-texto-suave", "--color-fondo", 4.5),
          new Par("--color-texto-suave", "--color-superficie", 4.5),
          new Par("--color-titulo", "--color-fondo", 4.5),
          new Par("--color-enlace", "--color-fondo", 4.5),
          new Par("--color-enlace", "--color-superficie", 4.5),
          new Par("--color-texto-sobre-accion", "--color-accion-primaria", 4.5),
          new Par("--color-texto-sobre-accion", "--color-accion-primaria-activa", 4.5),
          new Par("--color-texto-sobre-secundaria", "--color-accion-secundaria", 4.5),
          new Par("--color-texto-sobre-secundaria", "--color-accion-secundaria-activa", 4.5),
          new Par("--color-destacado-texto", "--color-destacado-fondo", 4.5),
          new Par("--color-destacado-texto-suave", "--color-destacado-fondo", 4.5),
          new Par("--color-destacado-texto-suave", "--color-destacado-fondo-alto", 4.5),
          new Par("--color-marca-brillante", "--color-destacado-fondo", 4.5),
          new Par("--color-texto-sobre-accion", "--color-marca-alterna", 4.5),
          new Par("--color-texto-sobre-accion", "--color-marca", 4.5),
          new Par("--color-marca-texto", "--color-superficie", 4.5),
          new Par("--color-marca-texto", "--color-fondo", 4.5),
          new Par("--color-texto-sobre-error", "--color-error-texto", 4.5),
          new Par("--color-exito-texto", "--color-exito-fondo", 4.5),
          new Par("--color-info-texto", "--color-info-fondo", 4.5),
          new Par("--color-advertencia-texto", "--color-advertencia-fondo", 4.5),
          new Par("--color-error-texto", "--color-error-fondo", 4.5),
          new Par("--color-error-texto", "--color-fondo", 4.5),
          new Par("--color-borde-control", "--color-fondo", 3),
          new Par("--color-foco", "--color-fondo", 3),
          new Par("--color-foco", "--color-superficie", 3),
          // El marco común (Raíces cálida, iteración 2, casa): cabecera, pie y barra del menú.
          new Par("--color-marco-texto", "--color-marco", 4.5),
          new Par("--color-marco-enlace", "--color-marco", 4.5),
          new Par("--color-marco-texto", "--color-estrato", 4.5),
          new Par("--color-actual-texto", "--color-actual-fondo", 4.5));

  private static final Pattern COLOR_LITERAL =
      Pattern.compile(
          "#\\p{XDigit}{3,8}\\b|\\b(?:rgba?|hsla?|hwb|lab|lch|oklab|oklch|color)\\(",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern REFERENCIA = Pattern.compile("var\\(\\s*(--[\\w-]+)");
  private static final Pattern MEDIA = Pattern.compile("@media([^{]*)\\{");
  private static final Pattern ANCHO_EN_MEDIA =
      Pattern.compile("\\(\\s*(min|max)-width\\s*:\\s*([^)\\s]+)\\s*\\)");
  private static final Pattern QUIEBRE =
      Pattern.compile("--medida-quiebre-[\\w-]+\\s*:\\s*([^;]+);");
  private static final Pattern DECLARACION = Pattern.compile("(--[\\w-]+)\\s*:\\s*([^;]+);");
  private static final Pattern ESTILO_EN_LINEA =
      Pattern.compile("<style|\\sstyle\\s*=|th:style", Pattern.CASE_INSENSITIVE);
  private static final Pattern HEX = Pattern.compile("#(\\p{XDigit}{6}|\\p{XDigit}{3})");

  private record Par(String texto, String fondo, double minimo) {}

  @Test
  void soloTokensCssDefineColores() {
    List<String> fallas = new ArrayList<>();
    for (Path css : cssFueraDeTokens()) {
      boolean variante = css.startsWith(VARIANTES);
      List<String> lineas = lineas(css);
      for (int i = 0; i < lineas.size(); i++) {
        String linea = lineas.get(i).strip();
        boolean declaraValorBase = variante && linea.startsWith(PREFIJO_BASE);
        if (COLOR_LITERAL.matcher(linea).find() && !declaraValorBase) {
          fallas.add(ubicacion(css, i) + " color literal: " + linea);
        }
      }
    }
    assertThat(fallas)
        .as("Los colores van en tokens.css; una variante A/B solo redefine valores base --p-*")
        .isEmpty();
  }

  @Test
  void estilosUsanSoloTokensDeUso() {
    List<String> fallas = new ArrayList<>();
    for (Path css : cssFueraDeTokens()) {
      boolean variante = css.startsWith(VARIANTES);
      List<String> lineas = lineas(css);
      for (int i = 0; i < lineas.size(); i++) {
        String linea = lineas.get(i);
        if (linea.contains("--pico-")) {
          fallas.add(ubicacion(css, i) + " variable de Pico fuera de tokens.css");
        }
        Matcher referencia = REFERENCIA.matcher(linea);
        while (referencia.find()) {
          String token = referencia.group(1);
          boolean permitido =
              PREFIJOS_DE_USO.stream().anyMatch(token::startsWith)
                  || (variante && token.startsWith(PREFIJO_BASE));
          if (!permitido) {
            fallas.add(ubicacion(css, i) + " usa " + token + ", que no es un token de uso");
          }
        }
      }
    }
    assertThat(fallas).isEmpty();
  }

  @Test
  void mediaQueriesUsanSoloLosQuiebresDeTokens() {
    Set<String> quiebres = new HashSet<>();
    for (Path tokens : hojasDeTokens()) {
      Set<String> propios =
          QUIEBRE
              .matcher(leer(tokens))
              .results()
              .map(m -> m.group(1).strip())
              .collect(Collectors.toSet());
      assertThat(propios).as("%s declara --medida-quiebre-*", tokens).isNotEmpty();
      quiebres.addAll(propios);
    }

    List<String> fallas = new ArrayList<>();
    for (Path css : archivos(CSS, ".css")) {
      Matcher media = MEDIA.matcher(leer(css));
      while (media.find()) {
        Matcher ancho = ANCHO_EN_MEDIA.matcher(media.group(1));
        while (ancho.find()) {
          if (!"min".equals(ancho.group(1)) || !quiebres.contains(ancho.group(2))) {
            fallas.add(
                css
                    + ": @media"
                    + media.group(1).strip()
                    + " (permitidos: min-width "
                    + quiebres
                    + ")");
          }
        }
      }
    }
    assertThat(fallas).as("Mobile first y solo los quiebres de tokens.css").isEmpty();
  }

  @Test
  void plantillasSinEstilosEnLinea() {
    List<String> fallas = new ArrayList<>();
    for (Path plantilla : archivos(PLANTILLAS, HTML)) {
      List<String> lineas = lineas(plantilla);
      for (int i = 0; i < lineas.size(); i++) {
        if (ESTILO_EN_LINEA.matcher(lineas.get(i)).find()) {
          fallas.add(ubicacion(plantilla, i));
        }
      }
    }
    assertThat(fallas).as("Sin <style> ni atributos style: los estilos van en app.css").isEmpty();
  }

  @Test
  void paresDeTokensCumplenContrasteAA() {
    List<String> fallas = new ArrayList<>();
    for (Path hoja : hojasDeTokens()) {
      Map<String, String> tokens = new HashMap<>();
      DECLARACION
          .matcher(leer(hoja))
          .results()
          .forEach(m -> tokens.putIfAbsent(m.group(1), m.group(2).strip()));
      for (Par par : PARES) {
        double contraste =
            contraste(color(hoja, tokens, par.texto()), color(hoja, tokens, par.fondo()));
        if (contraste < par.minimo()) {
          fallas.add(
              "%s: %s sobre %s: %.2f (mínimo %.1f)"
                  .formatted(hoja, par.texto(), par.fondo(), contraste, par.minimo()));
        }
      }
    }
    assertThat(fallas).isEmpty();
  }

  // --- El cerco de la capa CUBE (D33, B0-07): ver docs/interfaz/componentes.md
  // ----------------------

  /**
   * Tope de clases que aparecen una sola vez en las hojas heredadas. Solo baja (backlog, regla 3).
   */
  private static final int TOPE_DE_CLASES_DE_UN_SOLO_USO = 159;

  private static final Path CATALOGO = Path.of("../docs/interfaz/componentes.md");
  private static final Path ESTILOS = PLANTILLAS.resolve("estilos.html");
  private static final Path COMPONENTES = CSS.resolve("componentes");
  private static final Path CLASES_HEREDADAS =
      Path.of("src/test/resources/verificacion/clases-heredadas.txt");

  /** Hojas de la capa CUBE: no cuentan como heredadas ni como hojas de pantalla. */
  private static final Set<String> HOJAS_CUBE =
      Set.of("composicion.css", "utilidades.css", "movimiento.css");

  /** Hojas que no son de pantalla: la base y las capas. */
  private static final Set<String> HOJAS_DE_BASE = Set.of("app.css", TOKENS);

  /**
   * Las hojas de pantalla y compartidas que existían el 2026-11-04. Una hoja nueva que no es de
   * esta lista nace en la capa CUBE: solo disposición, sin color, fuente ni movimiento. La lista
   * solo baja: una hoja migrada por completo sale de acá.
   */
  private static final Set<String> HOJAS_HEREDADAS =
      Set.of(
          "admin-inicio.css",
          "donante-comun.css",
          "donante-donaciones-detalle.css",
          "donante-donaciones.css",
          "donante-entidades.css",
          "donante-inicio.css",
          "donante-notificaciones.css",
          "donante-perfil.css",
          "donante-seguimiento.css",
          "entidad-comun.css",
          "entidad-inicio.css",
          "entidad-necesidades.css",
          "entidad-notificaciones.css",
          "entidad-recibidas.css",
          "entidad-seguimiento-confirmar.css",
          "formularios.css",
          "privado.css",
          "publico-donaciones.css",
          "publico-entidades.css",
          "publico-ingresar.css",
          "publico-listas.css",
          "publico-mapa.css",
          "publico-navegacion.css",
          "publico-paginas.css",
          "publico-registro.css",
          "sellos.css");

  private static final Pattern COMENTARIO_CSS = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
  private static final Pattern TEXTO_CSS = Pattern.compile("\"[^\"]*\"");
  private static final Pattern URL_CSS = Pattern.compile("url\\([^)]*\\)");
  private static final Pattern CLASE_CSS = Pattern.compile("\\.([A-Za-z_][\\w-]*)");
  private static final Pattern DURACION =
      Pattern.compile("(?<![\\w.-])(?:\\d+\\.?\\d*|\\.\\d+)m?s\\b");
  private static final Pattern CURVA =
      Pattern.compile("cubic-bezier\\(|steps\\(|\\bease(?:-in-out|-in|-out)?\\b");
  private static final Pattern DE_PANTALLA_PROHIBIDO =
      Pattern.compile(
          "(?<![\\w-])(?:color|background(?:-color|-image)?|font(?:-family)?)\\s*:|@keyframes");
  private static final Pattern SOLO_DE_MOVIMIENTO_CSS =
      Pattern.compile(
          "@keyframes|--movimiento-(?:duracion|curva)-[a-z]+|--movimiento-retraso-(?!ninguno)[a-z]+");

  /** Hojas que pueden declarar @keyframes y usar duraciones y curvas: la base y las primitivas. */
  private static final Set<String> HOJAS_DE_MOVIMIENTO = Set.of("app.css", "movimiento.css");

  private static final Pattern COMENTARIO_THYMELEAF =
      Pattern.compile("<!--/\\*.*?\\*/-->", Pattern.DOTALL);
  private static final Pattern CLASE_ESTATICA = Pattern.compile("(?<![\\w:-])class=\"([^\"]*)\"");
  private static final Pattern CLASE_DE_THYMELEAF =
      Pattern.compile("th:class(?:append)?=\"([^\"]*)\"");
  private static final Pattern TEXTO_ENTRE_COMILLAS = Pattern.compile("'([^']*)'");
  private static final Pattern NOMBRE_DE_CLASE = Pattern.compile("[A-Za-z_][\\w-]*");
  private static final Pattern MOVIMIENTO_PEDIDO = Pattern.compile("data-movimiento=\"([a-z]+)\"");

  @Test
  void clasesDeLasPlantillasEstanEnElCatalogoOEnLasHeredadas() {
    Set<String> usadas = clasesUsadasEnPlantillas(PLANTILLAS);
    Set<String> heredadas = heredadas();
    assertThat(clasesFueraDelCatalogo(usadas, clasesCube().keySet(), heredadas))
        .as(
            "Una clase nueva entra primero a docs/interfaz/componentes.md y a /estilos, en la capa"
                + " CUBE; la lista de heredadas no se amplía")
        .isEmpty();
    assertThat(heredadas.stream().filter(clase -> !usadas.contains(clase)).sorted().toList())
        .as("Clases heredadas que ya no usa ninguna plantilla: sacarlas de %s", CLASES_HEREDADAS)
        .isEmpty();
  }

  @Test
  void laCapaCubeEstaEnElCatalogoYEnEstilos() {
    String catalogo = leer(CATALOGO);
    String muestras =
        leer(ESTILOS)
            + archivos(PLANTILLAS.resolve("componentes"), HTML).stream()
                .map(InterfazFitnessTest::leer)
                .collect(Collectors.joining("\n"));
    List<String> fallas = new ArrayList<>();
    clasesCube()
        .forEach(
            (clase, componente) -> {
              boolean enCatalogo =
                  palabra(clase).matcher(catalogo).find()
                      || (componente != null && clase.startsWith(componente + "-"));
              if (!enCatalogo) {
                fallas.add(clase + " no figura en " + CATALOGO);
              }
              if (!palabra(clase).matcher(muestras).find()) {
                fallas.add(clase + " no figura en /estilos (templates/estilos.html)");
              }
            });
    String movimiento = leer(CSS.resolve("movimiento.css"));
    MOVIMIENTO_PEDIDO
        .matcher(movimiento)
        .results()
        .map(m -> "data-movimiento=\"" + m.group(1) + "\"")
        .distinct()
        .forEach(
            pedido -> {
              if (!catalogo.contains(pedido)) {
                fallas.add(pedido + " no figura en " + CATALOGO);
              }
              if (!muestras.contains(pedido)) {
                fallas.add(pedido + " no figura en /estilos");
              }
            });
    assertThat(fallas).isEmpty();
  }

  @Test
  void clasesDeUnSoloUsoNoSuperanElTope() {
    List<String> hojas =
        archivos(CSS, ".css").stream()
            .filter(css -> css.getParent().equals(CSS))
            .filter(css -> !HOJAS_CUBE.contains(css.getFileName().toString()))
            .map(InterfazFitnessTest::leer)
            .toList();
    assertThat(clasesDeUnSoloUso(hojas))
        .as("El tope solo baja: al cerrar un ítem del backlog, bajarlo al valor medido")
        .isLessThanOrEqualTo(TOPE_DE_CLASES_DE_UN_SOLO_USO);
  }

  @Test
  void hojasDePantallaNuevasSoloDisponen() {
    List<String> fallas = new ArrayList<>();
    for (Path css : archivos(CSS, ".css")) {
      String nombre = css.getFileName().toString();
      boolean deLaCarpetaRaiz = css.getParent().equals(CSS);
      if (deLaCarpetaRaiz
          && !HOJAS_HEREDADAS.contains(nombre)
          && !HOJAS_CUBE.contains(nombre)
          && !HOJAS_DE_BASE.contains(nombre)) {
        fallas.addAll(
            declaracionesProhibidasEnPantalla(leer(css)).stream()
                .map(falla -> css + ": " + falla)
                .toList());
      }
    }
    assertThat(fallas)
        .as("Una hoja de pantalla nueva solo dispone; lo demás va a la capa CUBE")
        .isEmpty();
  }

  @Test
  void sinDuracionesNiCurvasLiteralesFueraDeTokens() {
    List<String> fallas = new ArrayList<>();
    for (Path css : cssFueraDeTokens()) {
      if (!css.startsWith(VARIANTES)) {
        fallas.addAll(
            movimientoLiteral(leer(css)).stream().map(falla -> css + ": " + falla).toList());
      }
    }
    assertThat(fallas)
        .as("Duraciones y curvas salen de los tokens --movimiento-* de tokens.css")
        .isEmpty();
  }

  /** Fase 1 del backlog: los @keyframes y los tiempos viven solo en movimiento.css. */
  @Test
  void keyframesYTiemposDeMovimientoSoloEnMovimientoCss() {
    List<String> fallas = new ArrayList<>();
    for (Path css : cssFueraDeTokens()) {
      boolean deLaCarpetaRaiz = css.getParent().equals(CSS);
      if (deLaCarpetaRaiz && !HOJAS_DE_MOVIMIENTO.contains(css.getFileName().toString())) {
        fallas.addAll(
            movimientoFueraDeSuHoja(leer(css)).stream().map(falla -> css + ": " + falla).toList());
      }
    }
    assertThat(fallas)
        .as(
            "Una animación nueva se agrega a movimiento.css y se referencia con --movimiento-animacion-*")
        .isEmpty();
  }

  @Test
  void alpineNoHaceLoQueElAdrProhibe() {
    String codigo =
        leer(RECURSOS.resolve("static/js/interfaz.js")).replaceAll("(?m)^\\s*//.*$", "");
    assertThat(codigo).doesNotContainPattern("fetch\\(|XMLHttpRequest|localStorage|sessionStorage");
    assertThat(
            archivos(PLANTILLAS, HTML).stream()
                .filter(plantilla -> leer(plantilla).contains("x-html"))
                .toList())
        .as("x-html está prohibido (D31)")
        .isEmpty();
  }

  @Test
  void lasSkillsDeInterfazEmpiezanPorElCatalogo() {
    for (String skill : List.of("vista-ui", "revision-ui")) {
      Path ruta = Path.of("../.agents/skills", skill, "SKILL.md");
      assertThat(leer(ruta)).as("%s cita el catálogo", ruta).contains("componentes.md");
    }
  }

  @Test
  void laNavegacionEntrePaginasRespetaReducirMovimiento() {
    String app = leer(CSS.resolve("app.css"));
    assertThat(app)
        .as("@view-transition va solo con prefers-reduced-motion: no-preference")
        .containsPattern(
            "@media \\(prefers-reduced-motion: no-preference\\)\\s*\\{[^@]*@view-transition\\s*\\{"
                + "\\s*navigation: auto");
    assertThat(leer(PLANTILLAS.resolve("interaccion.html")))
        .as("HTMX hace sus intercambios con View Transitions")
        .contains("globalViewTransitions");
  }

  /** Los casos inventados: cada regla del cerco tiene que fallar cuando se la rompe. */
  @Test
  void elCercoFallaConUnCasoInventadoYPasaConUnoSano() {
    assertThat(movimientoLiteral("a { transition: opacity 0.2s; }")).hasSize(1);
    assertThat(movimientoLiteral("a { animation: x 1s ease; }")).hasSize(2);
    assertThat(movimientoLiteral("a { transition: opacity var(--movimiento-duracion-corta); }"))
        .isEmpty();
    assertThat(movimientoLiteral("a { min-height: 100svh; margin: 0.5rem; /* 2s */ }")).isEmpty();

    assertThat(movimientoFueraDeSuHoja("@keyframes a { from { opacity: 0 } }")).hasSize(1);
    assertThat(
            movimientoFueraDeSuHoja("a { transition: opacity var(--movimiento-duracion-corta); }"))
        .hasSize(1);
    assertThat(movimientoFueraDeSuHoja("a { animation: x var(--movimiento-curva-base); }"))
        .hasSize(1);
    assertThat(movimientoFueraDeSuHoja("a { animation-delay: var(--movimiento-retraso-sello); }"))
        .hasSize(1);
    assertThat(
            movimientoFueraDeSuHoja(
                "a { animation: var(--movimiento-animacion-registro); animation-delay:"
                    + " var(--movimiento-retraso-ninguno); /* @keyframes */ }"))
        .isEmpty();

    assertThat(declaracionesProhibidasEnPantalla(".x { color: red; }")).hasSize(1);
    assertThat(declaracionesProhibidasEnPantalla("@keyframes a { from { opacity: 0 } }"))
        .hasSize(1);
    assertThat(declaracionesProhibidasEnPantalla(".x { border-color: red; display: grid; }"))
        .isEmpty();

    assertThat(clasesDeUnSoloUso(List.of(".a, .b { x: y } .a { z: w }"))).isEqualTo(1);
    assertThat(clasesDeUnSoloUso(List.of("/* .c */ .d { src: url(f.woff2); margin: 1.5rem }")))
        .isEqualTo(1);

    assertThat(
            clasesFueraDelCatalogo(
                Set.of("pila", "nueva", "vieja"), Set.of("pila"), Set.of("vieja")))
        .containsExactly("nueva");
  }

  /** Las declaraciones que una hoja de pantalla nueva no puede tener (D33). */
  static List<String> declaracionesProhibidasEnPantalla(String css) {
    return DE_PANTALLA_PROHIBIDO
        .matcher(sinComentariosCss(css))
        .results()
        .map(coincidencia -> coincidencia.group().strip())
        .toList();
  }

  /** Duraciones y curvas escritas a mano, en una hoja que no es tokens.css (D32). */
  static List<String> movimientoLiteral(String css) {
    String limpio = sinComentariosCss(css);
    List<String> fallas = new ArrayList<>();
    DURACION.matcher(limpio).results().forEach(m -> fallas.add("duración literal " + m.group()));
    CURVA.matcher(limpio).results().forEach(m -> fallas.add("curva literal " + m.group()));
    return fallas;
  }

  /**
   * @keyframes, duraciones y curvas de movimiento escritos fuera de movimiento.css y app.css.
   */
  static List<String> movimientoFueraDeSuHoja(String css) {
    return SOLO_DE_MOVIMIENTO_CSS
        .matcher(sinComentariosCss(css))
        .results()
        .map(java.util.regex.MatchResult::group)
        .toList();
  }

  /** Clases que aparecen una sola vez entre todas las hojas, sin comentarios, urls ni textos. */
  static long clasesDeUnSoloUso(List<String> hojas) {
    Map<String, Integer> veces = new HashMap<>();
    for (String css : hojas) {
      CLASE_CSS
          .matcher(sinTextosCss(css))
          .results()
          .forEach(m -> veces.merge(m.group(1), 1, Integer::sum));
    }
    return veces.values().stream().filter(cantidad -> cantidad == 1).count();
  }

  static List<String> clasesFueraDelCatalogo(
      Set<String> usadas, Set<String> cube, Set<String> heredadas) {
    return usadas.stream()
        .filter(clase -> !cube.contains(clase) && !heredadas.contains(clase))
        .sorted()
        .toList();
  }

  /**
   * Sin comentarios, textos ni urls: los textos primero, porque un data: URI lleva url() adentro.
   */
  private static String sinTextosCss(String css) {
    String sinTextos = TEXTO_CSS.matcher(sinComentariosCss(css)).replaceAll("");
    return URL_CSS.matcher(sinTextos).replaceAll("");
  }

  private static String sinComentariosCss(String css) {
    return COMENTARIO_CSS.matcher(css).replaceAll("");
  }

  private static Pattern palabra(String clase) {
    return Pattern.compile("(?<![\\w-])" + Pattern.quote(clase) + "(?![\\w-])");
  }

  /** Clase declarada en la capa CUBE → su componente (la hoja de componentes/), o null. */
  private static Map<String, String> clasesCube() {
    Map<String, String> clases = new HashMap<>();
    List<Path> hojas = new ArrayList<>(HOJAS_CUBE.stream().map(CSS::resolve).toList());
    if (Files.isDirectory(COMPONENTES)) {
      hojas.addAll(archivos(COMPONENTES, ".css"));
    }
    for (Path hoja : hojas) {
      String nombre = hoja.getFileName().toString().replace(".css", "");
      String componente = hoja.getParent().equals(COMPONENTES) ? nombre : null;
      CLASE_CSS
          .matcher(sinTextosCss(leer(hoja)))
          .results()
          .forEach(m -> clases.put(m.group(1), componente));
    }
    return clases;
  }

  private static Set<String> heredadas() {
    return lineas(CLASES_HEREDADAS).stream()
        .map(String::strip)
        .filter(linea -> !linea.isEmpty() && !linea.startsWith("#"))
        .collect(Collectors.toSet());
  }

  /** Las clases escritas a mano en las plantillas: class="…" y los textos de th:class(append). */
  private static Set<String> clasesUsadasEnPlantillas(Path carpeta) {
    Set<String> clases = new HashSet<>();
    for (Path plantilla : archivos(carpeta, HTML)) {
      String html = COMENTARIO_THYMELEAF.matcher(leer(plantilla)).replaceAll("");
      List<String> listas = new ArrayList<>();
      CLASE_ESTATICA.matcher(html).results().forEach(m -> listas.add(m.group(1)));
      CLASE_DE_THYMELEAF
          .matcher(html)
          .results()
          .forEach(
              m ->
                  TEXTO_ENTRE_COMILLAS
                      .matcher(m.group(1))
                      .results()
                      .forEach(texto -> listas.add(texto.group(1))));
      for (String lista : listas) {
        for (String clase : lista.split("\\s+")) {
          if (NOMBRE_DE_CLASE.matcher(clase).matches()) {
            clases.add(clase);
          }
        }
      }
    }
    return clases;
  }

  /** Resuelve var(--x) hasta un #rrggbb y devuelve su luminancia relativa (WCAG). */
  private static double color(Path hoja, Map<String, String> tokens, String token) {
    String valor = tokens.get(token);
    assertThat(valor).as("%s no está declarado en %s", token, hoja).isNotNull();
    Matcher referencia = REFERENCIA.matcher(valor);
    if (referencia.lookingAt()) {
      return color(hoja, tokens, referencia.group(1));
    }
    Matcher hex = HEX.matcher(valor);
    assertThat(hex.matches()).as("%s = %s: se espera #rrggbb", token, valor).isTrue();
    String digitos = hex.group(1);
    if (digitos.length() == 3) {
      digitos = digitos.replaceAll("(.)", "$1$1");
    }
    int rgb = Integer.parseInt(digitos, 16);
    return 0.2126 * canal(rgb >> 16) + 0.7152 * canal(rgb >> 8) + 0.0722 * canal(rgb);
  }

  private static double canal(int valor) {
    double c = (valor & 0xFF) / 255.0;
    return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  }

  private static double contraste(double luminanciaA, double luminanciaB) {
    return (Math.max(luminanciaA, luminanciaB) + 0.05)
        / (Math.min(luminanciaA, luminanciaB) + 0.05);
  }

  /** Las hojas llamadas tokens.css de static/css/. */
  private static List<Path> hojasDeTokens() {
    return archivos(CSS, ".css").stream().filter(InterfazFitnessTest::esTokens).toList();
  }

  private static List<Path> cssFueraDeTokens() {
    return archivos(CSS, ".css").stream().filter(css -> !esTokens(css)).toList();
  }

  private static boolean esTokens(Path css) {
    return css.getFileName().toString().equals(TOKENS);
  }

  private static List<Path> archivos(Path carpeta, String extension) {
    try (Stream<Path> recorrido = Files.walk(carpeta)) {
      return recorrido.filter(archivo -> archivo.toString().endsWith(extension)).sorted().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String leer(Path archivo) {
    try {
      return Files.readString(archivo, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<String> lineas(Path archivo) {
    return leer(archivo).lines().toList();
  }

  private static String ubicacion(Path archivo, int indice) {
    return archivo + ":" + (indice + 1);
  }
}
