package grupo5.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Reglas de la interfaz que se pueden comprobar sin navegador (cliente-liviano/AGENTS.md). Las
 * reglas y las listas permitidas viven solo en este archivo; la voz propia de cada alternativa, en
 * {@code src/test/resources/voz/<alternativa>.txt}.
 */
class InterfazFitnessTest {

  private static final Path RECURSOS = Path.of("src/main/resources");
  private static final Path CSS = RECURSOS.resolve("static/css");
  private static final String TOKENS = "tokens.css";
  private static final String HTML = ".html";
  private static final Path VARIANTES = CSS.resolve("variantes");
  private static final Path PLANTILLAS = RECURSOS.resolve("templates");

  /** El juego de plantillas de cada alternativa (estructura-cozy-medieval.md). */
  private static final Path JUEGOS = PLANTILLAS.resolve("alternativas");

  private static final Path ESTRUCTURAS_AB = PLANTILLAS.resolve("variantes");
  private static final String ALTERNATIVAS = "alternativas";

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
   * 1.4.11). Un par nuevo de colores en tokens.css se agrega acá. Rigen para el tokens.css de cada
   * alternativa: static/css/ (Glassmorphism) y static/css/alternativas/<nombre>/.
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
          new Par("--color-foco", "--color-superficie", 3));

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

  /** Textos de una plantilla: lo que se lee o se escucha. */
  private static final Pattern COMENTARIO_HTML = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

  private static final Pattern NODO_DE_TEXTO = Pattern.compile(">([^<>]+)<");
  private static final Pattern ATRIBUTO_DE_TEXTO =
      Pattern.compile(
          "\\s(?:th:)?(?:aria-label|alt|title|placeholder|data-etiqueta)\\s*=\\s*\"([^\"]*)\"");
  private static final Pattern ATRIBUTO_TH = Pattern.compile("\\sth:[\\w-]+\\s*=\\s*\"([^\"]*)\"");
  private static final Pattern LITERAL = Pattern.compile("'([^']*)'|\\|([^|]*)\\|");

  /**
   * Un literal de un atributo th:* es un texto si tiene mayúsculas, acentos o dos palabras; si no,
   * es un nombre técnico (un ícono, un id, un tipo) que cada alternativa puede cambiar.
   */
  private static final Pattern PARECE_TEXTO =
      Pattern.compile("\\p{Lu}|[^\\p{ASCII}]|\\p{L}{2,}[,.;:]?\\s+\\p{L}");

  /**
   * Voz propia (brief de cada alternativa, Voz): títulos y rótulos que una alternativa dice a su
   * modo. Una línea por texto, {@code <Glassmorphism> => <alternativa>}; un lado vacío es un rótulo
   * que la alternativa suma o que no usa.
   */
  private static final Path VOCES = Path.of("src/test/resources/voz");

  private static final String FLECHA_DE_VOZ = "=>";
  private static final Pattern NUMERO = Pattern.compile("\\d");

  /** Controles: lo que se toca. Su nombre accesible y su destino no cambian con la alternativa. */
  private static final Pattern CONTROL =
      Pattern.compile(
          "<(a|button|summary|label|select|textarea)\\b([^>]*)>(.*?)</\\1>", Pattern.DOTALL);

  private static final Pattern CONTROL_SIN_CIERRE = Pattern.compile("<(input|form)\\b([^>]*)>");
  private static final Pattern DESTINO =
      Pattern.compile(
          "\\s((?:th:)?(?:href|action|method|for|form|name|type|commandfor|command)|hx-[\\w-]+)"
              + "\\s*=\\s*\"([^\"]*)\"");
  private static final Pattern TEXTO_TH = Pattern.compile("\\sth:(?:u)?text\\s*=\\s*\"([^\"]*)\"");

  /**
   * Datos del modelo que se muestran: las expresiones ${…} de los atributos que escriben texto y de
   * los argumentos de los fragmentos. Las de forma (th:classappend, th:if, th:each, th:attr…) y las
   * de los íconos, que son siempre decorativos, no cuentan: un dibujo es propio de cada
   * alternativa.
   */
  private static final Pattern ATRIBUTO_DE_DATOS =
      Pattern.compile(
          "\\sth:(?:u?text|value|aria-label|alt|title|placeholder|datetime|src|data-etiqueta"
              + "|replace|insert)\\s*=\\s*\"([^\"]*)\"");

  private static final String ICONO = "fragmentos/icono ::";

  private static final Pattern EXPRESION = Pattern.compile("\\$\\{([^}]*)}");

  private record Par(String texto, String fondo, double minimo) {}

  /** Un texto de Glassmorphism y cómo lo dice la alternativa; vacío, si no lo tiene. */
  private record Voz(String comun, String propia) {}

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
  void cadaFragmentoFiguraEnElCatalogoDeSuAlternativa() {
    List<String> faltan = new ArrayList<>(faltanEnElCatalogo(PLANTILLAS, ""));
    for (Path juego : juegosDeAlternativas()) {
      faltan.addAll(faltanEnElCatalogo(juego, prefijo(juego)));
    }
    assertThat(faltan).as("Componentes que faltan en el /estilos de su alternativa").isEmpty();
  }

  @Test
  void cadaAlternativaTieneSuJuegoCompletoDePlantillas() {
    List<String> fallas = new ArrayList<>();
    Set<String> conHojas = new TreeSet<>();
    for (Path tokens : hojasDeTokens()) {
      if (tokens.getParent().getParent().endsWith(ALTERNATIVAS)) {
        conHojas.add(tokens.getParent().getFileName().toString());
      }
    }
    Set<String> conPlantillas = new TreeSet<>();
    juegosDeAlternativas().forEach(juego -> conPlantillas.add(juego.getFileName().toString()));
    if (!conHojas.equals(conPlantillas)) {
      fallas.add("alternativas con hojas " + conHojas + " y con plantillas " + conPlantillas);
    }

    Set<String> comunes = new TreeSet<>();
    plantillasDeGlassmorphism().forEach(plantilla -> comunes.add(relativa(PLANTILLAS, plantilla)));
    for (Path juego : juegosDeAlternativas()) {
      Set<String> propias = new TreeSet<>();
      archivos(juego, HTML).forEach(plantilla -> propias.add(relativa(juego, plantilla)));
      diferencia(juego.toString(), comunes, propias, fallas);
    }
    assertThat(fallas)
        .as("Cada plantilla tiene su gemela en cada alternativa, y al revés")
        .isEmpty();
  }

  @Test
  void lasGemelasTienenLosMismosDatosYAcciones() {
    List<String> fallas = new ArrayList<>();
    for (Path juego : juegosDeAlternativas()) {
      List<Voz> voz = voz(juego);
      for (Path comun : plantillasDeGlassmorphism()) {
        Path gemela = juego.resolve(PLANTILLAS.relativize(comun));
        if (Files.exists(gemela)) {
          diferencia(
              gemela + " (textos)",
              conVoz(textos(comun), voz),
              sinRotulosNuevos(textos(gemela), voz),
              fallas);
          diferencia(gemela + " (datos)", datos(comun), datos(gemela), fallas);
          diferencia(gemela + " (controles)", controles(comun), controles(gemela), fallas);
        }
      }
    }
    assertThat(fallas)
        .as(
            "Mismos datos, acciones y nombres de controles en todas las alternativas; los títulos y"
                + " rótulos propios van en src/test/resources/voz/<alternativa>.txt")
        .isEmpty();
  }

  @Test
  void laVozPropiaSoloCambiaTitulosYRotulos() {
    List<String> fallas = new ArrayList<>();
    for (Path juego : juegosDeAlternativas()) {
      List<String> comunes = new ArrayList<>();
      List<String> propios = new ArrayList<>();
      for (Path comun : plantillasDeGlassmorphism()) {
        Path gemela = juego.resolve(PLANTILLAS.relativize(comun));
        if (Files.exists(gemela)) {
          comunes.addAll(textos(comun));
          propios.addAll(textos(gemela));
        }
      }
      for (Voz linea : voz(juego)) {
        String donde = juego.getFileName() + ": " + linea;
        if (NUMERO.matcher(linea.comun() + linea.propia()).find()) {
          fallas.add(donde + " tiene números, que son datos");
        }
        boolean usada =
            linea.comun().isEmpty()
                ? propios.contains(linea.propia())
                : comunes.contains(linea.comun());
        if (!usada) {
          fallas.add(donde + " no aparece en ninguna plantilla");
        }
      }
    }
    assertThat(fallas).as("La voz propia: rótulos sin números y en uso").isEmpty();
  }

  @Test
  void lasPlantillasDeUnaAlternativaUsanSoloLasSuyas() {
    List<String> fallas = new ArrayList<>();
    for (Path juego : juegosDeAlternativas()) {
      // ~{x :: y} o la forma corta th:replace="x :: y"; no cuentan las expresiones ${…} ni ::y.
      Pattern ajena =
          Pattern.compile(
              "(?:~\\{|th:(?:replace|insert)\\s*=\\s*\"(?![~$@#*]\\{))(?!::)(?!"
                  + Pattern.quote(prefijo(juego))
                  + ")");
      for (Path plantilla : archivos(juego, HTML)) {
        List<String> lineas = lineas(plantilla);
        for (int i = 0; i < lineas.size(); i++) {
          if (ajena.matcher(lineas.get(i)).find()) {
            fallas.add(ubicacion(plantilla, i) + ": " + lineas.get(i).strip());
          }
        }
      }
    }
    assertThat(fallas).as("Referencias a plantillas de otra alternativa").isEmpty();
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

  private static List<String> faltanEnElCatalogo(Path juego, String prefijo) {
    Path catalogo = juego.resolve("paginas/estilos.html");
    String texto = leer(catalogo);
    return archivos(juego.resolve("fragmentos"), HTML).stream()
        .map(fragmento -> fragmento.getFileName().toString().replace(HTML, ""))
        .filter(nombre -> !texto.contains("~{" + prefijo + "fragmentos/" + nombre + " ::"))
        .map(nombre -> catalogo + ": " + nombre)
        .toList();
  }

  /** Nodos de texto, atributos que se leen y literales que parecen texto en atributos th:*. */
  private static Set<String> textos(Path plantilla) {
    String html = sinComentarios(plantilla);
    Set<String> textos = new TreeSet<>();
    NODO_DE_TEXTO.matcher(html).results().forEach(m -> agregarTexto(textos, m.group(1)));
    ATRIBUTO_DE_TEXTO.matcher(html).results().forEach(m -> agregarTexto(textos, m.group(1)));
    ATRIBUTO_TH
        .matcher(html)
        .results()
        .flatMap(atributo -> LITERAL.matcher(atributo.group(1)).results())
        .map(literal -> literal.group(1) != null ? literal.group(1) : literal.group(2))
        .filter(literal -> PARECE_TEXTO.matcher(literal).find())
        .forEach(literal -> agregarTexto(textos, literal));
    return textos;
  }

  private static void agregarTexto(Set<String> textos, String texto) {
    String limpio = texto.strip().replaceAll("\\s+", " ");
    if (!limpio.isEmpty()) {
      textos.add(limpio);
    }
  }

  /** Los textos de Glassmorphism dichos con la voz de la alternativa. */
  private static Set<String> conVoz(Set<String> comunes, List<Voz> voz) {
    Set<String> dichos = new TreeSet<>();
    for (String texto : comunes) {
      String propio =
          voz.stream()
              .filter(linea -> linea.comun().equals(texto))
              .map(Voz::propia)
              .findFirst()
              .orElse(texto);
      if (!propio.isEmpty()) {
        dichos.add(propio);
      }
    }
    return dichos;
  }

  /** Los textos de la alternativa sin los rótulos que suma su voz. */
  private static Set<String> sinRotulosNuevos(Set<String> propios, List<Voz> voz) {
    Set<String> sinNuevos = new TreeSet<>(propios);
    voz.stream()
        .filter(linea -> linea.comun().isEmpty())
        .forEach(linea -> sinNuevos.remove(linea.propia()));
    return sinNuevos;
  }

  private static List<Voz> voz(Path juego) {
    Path tabla = VOCES.resolve(juego.getFileName() + ".txt");
    if (!Files.exists(tabla)) {
      return List.of();
    }
    List<Voz> voz = new ArrayList<>();
    for (String linea : lineas(tabla)) {
      String limpia = linea.strip();
      if (limpia.isEmpty() || limpia.startsWith("#")) {
        continue;
      }
      int flecha = limpia.indexOf(FLECHA_DE_VOZ);
      assertThat(flecha).as("%s: falta %s en «%s»", tabla, FLECHA_DE_VOZ, limpia).isNotNegative();
      voz.add(
          new Voz(
              limpia.substring(0, flecha).strip(),
              limpia.substring(flecha + FLECHA_DE_VOZ.length()).strip()));
    }
    return voz;
  }

  private static Set<String> datos(Path plantilla) {
    Set<String> datos = new TreeSet<>();
    ATRIBUTO_DE_DATOS
        .matcher(sinComentarios(plantilla))
        .results()
        .map(atributo -> atributo.group(1))
        .filter(valor -> !valor.contains(ICONO))
        .flatMap(valor -> EXPRESION.matcher(valor).results())
        .forEach(expresion -> datos.add(expresion.group(1).replaceAll("\\s+", "")));
    return datos;
  }

  /** Cada control, con su etiqueta, su destino y las piezas de su nombre accesible. */
  private static List<String> controles(Path plantilla) {
    String html = sinComentarios(plantilla);
    List<String> controles = new ArrayList<>();
    CONTROL
        .matcher(html)
        .results()
        .forEach(m -> controles.add(control(m.group(1), m.group(2), m.group(3))));
    CONTROL_SIN_CIERRE
        .matcher(html)
        .results()
        .forEach(m -> controles.add(control(m.group(1), m.group(2), "")));
    return controles;
  }

  /** Las piezas del nombre van en cualquier orden: cada alternativa las acomoda a su modo. */
  private static String control(String etiqueta, String atributos, String contenido) {
    Set<String> destino = new TreeSet<>();
    DESTINO.matcher(atributos).results().forEach(m -> destino.add(m.group(1) + "=" + m.group(2)));
    Set<String> nombre = new TreeSet<>();
    ATRIBUTO_DE_TEXTO.matcher(atributos).results().forEach(m -> agregarTexto(nombre, m.group(1)));
    TEXTO_TH
        .matcher(atributos + contenido)
        .results()
        .forEach(m -> agregarTexto(nombre, m.group(1)));
    NODO_DE_TEXTO
        .matcher(">" + contenido + "<")
        .results()
        .forEach(m -> agregarTexto(nombre, m.group(1)));
    return etiqueta + " " + destino + " " + nombre;
  }

  /** Lo que sobra o falta, contando las repeticiones. */
  private static void diferencia(
      String donde, Collection<String> esperados, Collection<String> propios, List<String> fallas) {
    List<String> faltan = new ArrayList<>(esperados);
    propios.forEach(faltan::remove);
    List<String> sobran = new ArrayList<>(propios);
    esperados.forEach(sobran::remove);
    if (!faltan.isEmpty() || !sobran.isEmpty()) {
      faltan.sort(null);
      sobran.sort(null);
      fallas.add(donde + ": faltan " + faltan + "; sobran " + sobran);
    }
  }

  private static String sinComentarios(Path plantilla) {
    return COMENTARIO_HTML.matcher(leer(plantilla)).replaceAll("");
  }

  /** Las plantillas de templates/ salvo los juegos de alternativas y las variantes A/B. */
  private static List<Path> plantillasDeGlassmorphism() {
    return archivos(PLANTILLAS, HTML).stream()
        .filter(plantilla -> !plantilla.startsWith(JUEGOS) && !plantilla.startsWith(ESTRUCTURAS_AB))
        .toList();
  }

  private static List<Path> juegosDeAlternativas() {
    if (!Files.isDirectory(JUEGOS)) {
      return List.of();
    }
    try (Stream<Path> carpetas = Files.list(JUEGOS)) {
      return carpetas.filter(Files::isDirectory).sorted().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Cómo nombran las plantillas de un juego a las suyas: ~{alternativas/<nombre>/...}. */
  private static String prefijo(Path juego) {
    return ALTERNATIVAS + "/" + juego.getFileName() + "/";
  }

  private static String relativa(Path base, Path archivo) {
    return base.relativize(archivo).toString().replace('\\', '/');
  }

  /** Un tokens.css por alternativa de identidad. */
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
