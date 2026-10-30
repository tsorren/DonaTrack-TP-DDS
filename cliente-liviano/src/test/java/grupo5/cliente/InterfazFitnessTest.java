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
  private static final Path VARIANTES = CSS.resolve("variantes");
  private static final Path PLANTILLAS = RECURSOS.resolve("templates");
  private static final Path FRAGMENTOS = PLANTILLAS.resolve("fragmentos");
  private static final Path CATALOGO = PLANTILLAS.resolve("paginas/estilos.html");

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
    for (Path plantilla : archivos(PLANTILLAS, ".html")) {
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
  void cadaFragmentoFiguraEnElCatalogo() {
    String catalogo = leer(CATALOGO);
    List<String> faltan =
        archivos(FRAGMENTOS, ".html").stream()
            .map(fragmento -> fragmento.getFileName().toString().replace(".html", ""))
            .filter(nombre -> !catalogo.contains("fragmentos/" + nombre + " ::"))
            .toList();
    assertThat(faltan).as("Componentes que faltan en /estilos (%s)", CATALOGO).isEmpty();
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
