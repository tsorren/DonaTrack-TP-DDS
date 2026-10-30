package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Comprobación visual (perfil Maven {@code visual}): por cada página de {@code paginas.txt}, en los
 * anchos de referencia, saca una captura y exige axe AA sin violaciones, controles de 48 × 48 px o
 * más y nada desbordado; en los anchos intermedios, solo que nada se desborde. Con {@code
 * -Dverificacion.variantes=<tema>-a,<tema>-b} hace lo mismo con cada variante y arma la
 * comparación. Resultado en {@code target/capturas/}: PNG, index.html, comparacion.html y
 * reporte.json.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class VerificacionVisualTest {

  private static final Path CAPTURAS = Path.of("target/capturas");
  private static final Path PAGINAS = Path.of("src/test/resources/verificacion/paginas.txt");
  private static final List<String> NORMAS_AXE =
      List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa");
  private static final int TOQUE_MINIMO = 48;

  /** Controles visibles de menos de 48 × 48 px; los enlaces dentro de un texto no cuentan. */
  private static final String CONTROLES_CHICOS =
      """
      minimo => [...document.querySelectorAll(
          'a[href], button, input:not([type=hidden]), select, textarea, summary, [role=button]')]
        .filter(control => !control.closest('p, small'))
        .map(control => [control, control.getBoundingClientRect()])
        .filter(([, caja]) => caja.width > 0 && caja.height > 0 && caja.bottom > 0)
        .filter(([, caja]) => caja.width < minimo || caja.height < minimo)
        .map(([control, caja]) => control.tagName.toLowerCase() + ' "'
          + (control.textContent || control.name || control.id).trim().slice(0, 40) + '" '
          + Math.round(caja.width) + 'x' + Math.round(caja.height))
      """;

  private static final String HAY_DESBORDE =
      "() => document.documentElement.scrollWidth > document.documentElement.clientWidth";

  /** La captura de página completa no dispara loading="lazy": se cargan todas antes. */
  private static final String CARGAR_IMAGENES =
      """
      async () => {
        const imagenes = [...document.images];
        imagenes.forEach(imagen => { imagen.loading = 'eager'; });
        await Promise.all(imagenes.filter(imagen => !imagen.complete).map(imagen =>
          new Promise(listo => { imagen.onload = listo; imagen.onerror = listo; })));
      }
      """;

  private static final Path VARIANTES_CSS = Path.of("src/main/resources/static/css/variantes");
  private static final Pattern COMENTARIO = Pattern.compile("/\\*(.*?)\\*/", Pattern.DOTALL);

  /**
   * Visor de capturas (index.html y comparacion.html), pensado para 1920 × 1080: índice de
   * secciones a la izquierda, una o dos variantes lado a lado, ancho y página a elección y
   * desplazamiento a la par. Un enlace con {@code #izq=<variante>&der=<variante>&ancho=<px>} abre
   * esa comparación. @TITULO@ y @DATOS@ (JSON) se completan al generarlo.
   */
  private static final String VISOR =
      """
      <!DOCTYPE html><html lang="es-AR"><head><meta charset="utf-8"><title>@TITULO@</title>
      <style>
      *{box-sizing:border-box}
      body{margin:0;font:15px/1.4 system-ui,sans-serif;color:#1d2327;background:#e3e7ea}
      header{display:flex;flex-wrap:wrap;align-items:center;gap:8px 20px;min-height:60px;
        padding:10px 16px;background:#fff;box-shadow:0 1px 6px #0003}
      h1{margin:0;font-size:17px}
      select,button{font:inherit;padding:5px 10px;border:1px solid #8a959a;border-radius:6px;
        background:#fff;cursor:pointer}
      button[aria-pressed=true]{background:#1d2327;border-color:#1d2327;color:#fff}
      .ayuda{color:#5b6469;font-size:13px}
      main{display:grid;grid-template-columns:230px 1fr 1fr;gap:12px;height:calc(100vh - 60px);
        padding:12px}
      main.una{grid-template-columns:230px 1fr}
      main.una #der-columna{display:none}
      nav{overflow:auto;padding:8px;background:#fff;border-radius:10px;box-shadow:0 1px 4px #0002}
      nav b{display:block;padding:6px 8px;font-size:13px;color:#5b6469}
      nav button{display:block;width:100%;margin:2px 0;border:0;text-align:left;
        background:transparent}
      nav button:hover{background:#eef1f3}
      section{display:flex;flex-direction:column;min-height:0;overflow:hidden;background:#fff;
        border-radius:10px;box-shadow:0 1px 4px #0002}
      section p{margin:0;padding:10px 14px;border-bottom:1px solid #d5dadd}
      section p b{display:block;font-size:16px}
      .lienzo{flex:1;overflow:auto;background:#f0f2f3}
      .lienzo img{display:block;width:100%;height:auto;margin:0 auto}
      .lienzo img.angosta{width:360px;box-shadow:0 0 0 1px #c9cfd2}
      </style></head><body>
      <header><h1>@TITULO@</h1>
      <label>Izquierda <select id="izq"></select></label>
      <label class="dos">Derecha <select id="der"></select></label>
      <label id="pagina-campo">Página <select id="pagina"></select></label>
      <span>Ancho <span id="anchos"></span></span>
      <label class="dos"><input type="checkbox" id="sincro" checked> Desplazamiento a la par</label>
      <span class="ayuda"><span class="dos">← → cambian la derecha · </span>1, 2 y 3 cambian el
        ancho</span>
      </header>
      <main id="principal">
      <nav aria-label="Secciones de la página"><b>Ir a</b><div id="secciones"></div></nav>
      <section><p id="izq-texto"></p><div class="lienzo" id="izq-lienzo"><img id="izq-img" alt=""></div></section>
      <section id="der-columna"><p id="der-texto"></p><div class="lienzo" id="der-lienzo"><img id="der-img" alt=""></div></section>
      </main>
      <script type="application/json" id="datos">@DATOS@</script>
      <script>
      const d = JSON.parse(document.getElementById('datos').textContent);
      const $ = id => document.getElementById(id);
      const paginas = Object.keys(d.capturas);
      const dos = d.variantes.length > 1;
      let ancho = d.anchos.includes(1280) ? 1280 : d.anchos[0];
      $('principal').classList.toggle('una', !dos);
      document.querySelectorAll('.dos').forEach(e => { e.hidden = !dos; });
      d.variantes.forEach((v, i) => { $('izq').add(new Option(v.nombre || 'actual', i)); $('der').add(new Option(v.nombre, i)); });
      $('der').selectedIndex = Math.min(1, d.variantes.length - 1);
      paginas.forEach(p => $('pagina').add(new Option(p, p)));
      $('pagina-campo').hidden = paginas.length < 2;
      d.anchos.forEach(a => {
        const b = document.createElement('button');
        b.textContent = a; b.dataset.ancho = a; b.onclick = () => { ancho = a; pintar(); };
        $('anchos').append(b, ' ');
      });
      const lados = () => dos ? ['izq', 'der'] : ['izq'];
      const captura = lado => d.capturas[$('pagina').value][d.variantes[$(lado).value].nombre][ancho];
      function irA(titulo) {
        for (const lado of lados()) {
          const img = $(lado + '-img'), s = captura(lado).secciones.find(x => x.titulo === titulo);
          if (s) $(lado + '-lienzo').scrollTop = s.y * img.clientWidth / img.naturalWidth - 8;
        }
      }
      function pintar() {
        for (const lado of lados()) {
          const v = d.variantes[$(lado).value], img = $(lado + '-img');
          img.src = captura(lado).archivo;
          img.alt = (v.nombre || 'Página actual') + ', ' + ancho + ' px';
          img.className = ancho < 600 ? 'angosta' : '';
          const titulo = document.createElement('b');
          titulo.textContent = v.nombre || 'Página actual';
          $(lado + '-texto').replaceChildren(titulo, v.descripcion);
        }
        $('secciones').replaceChildren(...captura('izq').secciones.map(s => {
          const b = document.createElement('button');
          b.textContent = s.titulo; b.onclick = () => irA(s.titulo);
          return b;
        }));
        document.querySelectorAll('[data-ancho]').forEach(b => b.setAttribute('aria-pressed', b.dataset.ancho == ancho));
      }
      function marcar() {
        const img = $('izq-img'), escala = img.clientWidth / img.naturalWidth;
        const arriba = $('izq-lienzo').scrollTop + 20;
        const actual = captura('izq').secciones.filter(s => s.y * escala <= arriba).pop();
        document.querySelectorAll('#secciones button').forEach(b =>
          b.setAttribute('aria-pressed', !!actual && b.textContent === actual.titulo));
      }
      let moviendo = false;
      for (const [este, otro] of [['izq-lienzo', 'der-lienzo'], ['der-lienzo', 'izq-lienzo']]) {
        $(este).addEventListener('scroll', () => {
          if (este === 'izq-lienzo') marcar();
          if (!dos || !$('sincro').checked || moviendo) return;
          moviendo = true;
          const a = $(este), b = $(otro);
          b.scrollTop = a.scrollTop / Math.max(1, a.scrollHeight - a.clientHeight) * (b.scrollHeight - b.clientHeight);
          requestAnimationFrame(() => { moviendo = false; });
        });
      }
      document.addEventListener('keydown', e => {
        if (e.target.tagName === 'SELECT') return;
        const n = d.variantes.length, der = $('der');
        if (dos && e.key === 'ArrowRight') der.selectedIndex = (der.selectedIndex + 1) % n;
        else if (dos && e.key === 'ArrowLeft') der.selectedIndex = (der.selectedIndex + n - 1) % n;
        else if (d.anchos[e.key - 1]) ancho = d.anchos[e.key - 1];
        else return;
        pintar();
      });
      ['izq', 'der', 'pagina'].forEach(id => $(id).addEventListener('change', pintar));
      function desdeEnlace() {
        const pedido = new URLSearchParams(location.hash.slice(1));
        const indice = lado => d.variantes.findIndex(v => v.nombre === pedido.get(lado));
        ['izq', 'der'].filter(lado => indice(lado) >= 0).forEach(lado => { $(lado).value = indice(lado); });
        if (d.anchos.includes(Number(pedido.get('ancho')))) ancho = Number(pedido.get('ancho'));
        pintar();
      }
      window.addEventListener('hashchange', desdeEnlace);
      desdeEnlace();
      </script></body></html>
      """;

  /** Título (h2) y altura de cada sección de main, para el índice del visor. */
  private static final String SECCIONES =
      """
      () => [...document.querySelectorAll('main > section > h2')].map(h => ({
        titulo: h.textContent.trim(),
        y: Math.round(h.getBoundingClientRect().top + window.scrollY)
      }))
      """;

  private record Captura(
      String ruta, String variante, int ancho, String archivo, Object secciones) {}

  record Falla(String ruta, String variante, int ancho, String tipo, String detalle) {}

  @LocalServerPort private int puerto;

  @Test
  void paginasCumplenElPisoEnTodosLosAnchos() throws IOException {
    List<String> variantes = variantes();
    List<Captura> capturas = new ArrayList<>();
    List<Falla> fallas = new ArrayList<>();
    Files.createDirectories(CAPTURAS);

    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina = navegador.newPage();
      for (String variante : variantes) {
        for (String ruta : paginas()) {
          for (int ancho : AnchosDeReferencia.REFERENCIA) {
            if (abrir(pagina, ruta, variante, ancho, fallas)) {
              String archivo = nombreDeCaptura(ruta, variante, ancho);
              pagina.evaluate(CARGAR_IMAGENES);
              pagina.screenshot(
                  new Page.ScreenshotOptions()
                      .setPath(CAPTURAS.resolve(archivo))
                      .setFullPage(true));
              capturas.add(new Captura(ruta, variante, ancho, archivo, pagina.evaluate(SECCIONES)));
              revisarAccesibilidad(pagina, ruta, variante, ancho, fallas);
              revisarToque(pagina, ruta, variante, ancho, fallas);
              revisarDesborde(pagina, ruta, variante, ancho, fallas);
            }
          }
          for (int ancho : AnchosDeReferencia.INTERMEDIOS) {
            if (abrir(pagina, ruta, variante, ancho, fallas)) {
              revisarDesborde(pagina, ruta, variante, ancho, fallas);
            }
          }
        }
      }
    }

    Files.writeString(
        CAPTURAS.resolve("index.html"), visor(capturas, variantes), StandardCharsets.UTF_8);
    if (variantes.size() > 1) {
      Files.writeString(
          CAPTURAS.resolve("comparacion.html"), visor(capturas, variantes), StandardCharsets.UTF_8);
    }
    JsonMapper.builder()
        .build()
        .writerWithDefaultPrettyPrinter()
        .writeValue(CAPTURAS.resolve("reporte.json"), fallas);

    assertThat(fallas).as("Detalle en %s", CAPTURAS.resolve("reporte.json")).isEmpty();
  }

  private boolean abrir(Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    pagina.setViewportSize(ancho, AnchosDeReferencia.ALTO);
    String separador = ruta.contains("?") ? "&" : "?";
    String url =
        "http://localhost:"
            + puerto
            + ruta
            + (variante.isEmpty() ? "" : separador + "variante=" + variante);
    Response respuesta = pagina.navigate(url);
    if (respuesta == null || !respuesta.ok()) {
      int estado = respuesta == null ? 0 : respuesta.status();
      fallas.add(new Falla(ruta, variante, ancho, "pagina", "HTTP " + estado + " en " + url));
      return false;
    }
    return true;
  }

  private static void revisarAccesibilidad(
      Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    for (Rule regla : new AxeBuilder(pagina).withTags(NORMAS_AXE).analyze().getViolations()) {
      for (CheckedNode nodo : regla.getNodes()) {
        fallas.add(
            new Falla(
                ruta,
                variante,
                ancho,
                "accesibilidad",
                regla.getId()
                    + " ("
                    + regla.getImpact()
                    + "): "
                    + regla.getHelp()
                    + " en "
                    + nodo.getTarget()));
      }
    }
  }

  private static void revisarToque(
      Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    Object chicos = pagina.evaluate(CONTROLES_CHICOS, TOQUE_MINIMO);
    if (chicos instanceof List<?> controles) {
      controles.forEach(
          control ->
              fallas.add(
                  new Falla(ruta, variante, ancho, "toque", "menos de 48x48 px: " + control)));
    }
  }

  private static void revisarDesborde(
      Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    if (Boolean.TRUE.equals(pagina.evaluate(HAY_DESBORDE))) {
      fallas.add(
          new Falla(ruta, variante, ancho, "desborde", "la página es más ancha que la pantalla"));
    }
  }

  private static List<String> paginas() throws IOException {
    return Files.readAllLines(PAGINAS, StandardCharsets.UTF_8).stream()
        .map(String::strip)
        .filter(linea -> !linea.isEmpty() && !linea.startsWith("#"))
        .toList();
  }

  /** Sin variantes, una sola pasada sobre las páginas tal como están (variante vacía). */
  private static List<String> variantes() {
    String valor = System.getProperty("verificacion.variantes", "");
    List<String> variantes =
        Arrays.stream(valor.split(",")).map(String::strip).filter(v -> !v.isEmpty()).toList();
    return variantes.isEmpty() ? List.of("") : variantes;
  }

  private static String nombreDeCaptura(String ruta, String variante, int ancho) {
    String base = ruta.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "");
    String sufijo = variante.isEmpty() ? "" : "--" + variante;
    return (base.isEmpty() ? "inicio" : base) + sufijo + "-" + ancho + ".png";
  }

  /**
   * Datos del visor: capturas por página, variante y ancho, con la descripción de cada variante.
   */
  private static String visor(List<Captura> capturas, List<String> variantes) throws IOException {
    Map<String, Map<String, Map<Integer, Map<String, Object>>>> porPagina = new LinkedHashMap<>();
    for (Captura captura : capturas) {
      porPagina
          .computeIfAbsent(captura.ruta(), ruta -> new LinkedHashMap<>())
          .computeIfAbsent(captura.variante(), variante -> new TreeMap<>())
          .put(
              captura.ancho(),
              Map.of("archivo", captura.archivo(), "secciones", captura.secciones()));
    }
    List<Map<String, String>> descritas = new ArrayList<>();
    for (String variante : variantes) {
      descritas.add(Map.of("nombre", variante, "descripcion", descripcion(variante)));
    }
    String datos =
        JsonMapper.builder()
            .build()
            .writeValueAsString(
                Map.of(
                    "variantes", descritas,
                    "capturas", porPagina,
                    "anchos", AnchosDeReferencia.REFERENCIA))
            .replace("</", "<\\/");
    return VISOR
        .replace("@TITULO@", HtmlUtils.htmlEscape(titulo(variantes)))
        .replace("@DATOS@", datos);
  }

  private static String titulo(List<String> variantes) {
    return variantes.size() > 1
        ? "Comparación: " + String.join(" / ", variantes)
        : "Capturas" + (variantes.get(0).isEmpty() ? "" : ": " + variantes.get(0));
  }

  /** Primer comentario del CSS de la variante, en una línea; vacío si no hay. */
  private static String descripcion(String variante) throws IOException {
    Path css = VARIANTES_CSS.resolve(variante + ".css");
    if (!Files.exists(css)) {
      return "";
    }
    Matcher comentario = COMENTARIO.matcher(Files.readString(css, StandardCharsets.UTF_8));
    return comentario.find() ? comentario.group(1).replaceAll("\\s*\\*\\s*", " ").strip() : "";
  }
}
