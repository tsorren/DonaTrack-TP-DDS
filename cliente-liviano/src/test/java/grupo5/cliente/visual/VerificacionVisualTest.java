package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.AxeResults;
import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Video;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.ScreenshotAnimations;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Comprobación visual (perfil Maven {@code visual}): por cada página de {@code paginas.txt}, en los
 * anchos de referencia, saca una captura y exige axe AA sin violaciones, controles de 48 × 48 px o
 * más y nada desbordado; en los anchos intermedios, solo que nada se desborde. En el celular
 * captura además la primera pantalla (360 × 640, {@code <página>-360x640.png}). Sin propiedades,
 * recorre el sitio. Con {@code -Dverificacion.variantes=<tema>-a,<tema>-b} hace lo mismo con cada
 * variante y las compara. Con {@code -Dverificacion.paginas=/a,/b} recorre esas rutas en lugar de
 * las de paginas.txt. Con {@code -Dverificacion.boceto=true}, solo capturas a 1280 px, sin
 * comprobar el piso (las vueltas de una exploración A/B). Resultado en {@code target/capturas/}:
 * PNG, index.html, comparacion.html y reporte.json. Las capturas congelan las animaciones de la
 * página, no las que van dentro de una imagen (el spinner de Pico): para comparar dos corridas,
 * {@code scripts/visual/Imagenes.java comparar} con carpeta de ruido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class VerificacionVisualTest {

  private static final Path CAPTURAS = Path.of("target/capturas");
  private static final Path PAGINAS = Path.of("src/test/resources/verificacion/paginas.txt");

  /**
   * Una ruta con {@code ?respuesta=404}, {@code 403} o {@code 500} pide en la maqueta una página de
   * error (RF-CL-08): ese código es el esperado, no una falla.
   */
  private static final Pattern RESPUESTA_PEDIDA = Pattern.compile("[?&]respuesta=(\\d{3})\\b");

  private static final List<String> NORMAS_AXE =
      List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa");
  private static final int TOQUE_MINIMO = 48;

  /**
   * Boceto ({@code -Dverificacion.boceto=true}): para las vueltas de una exploración A/B, solo la
   * captura a 1280 px, sin axe, toque, desborde, anchos intermedios ni primera pantalla. El piso se
   * comprueba con la corrida completa sobre la opción elegida.
   */
  private static final boolean BOCETO = Boolean.getBoolean("verificacion.boceto");

  private static final List<Integer> ANCHOS =
      BOCETO ? List.of(1280) : AnchosDeReferencia.REFERENCIA;

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

  /**
   * El contraste de un texto que axe deja sin medir (un pseudo-elemento, un degradado o una imagen
   * en el fondo): el color del texto sobre los fondos de color de sus ancestros, compuestos con su
   * transparencia. Si no llega a AA (4,5:1; 3:1 la letra grande) devuelve la razón y el mínimo como
   * texto; si llega o no se puede medir, null. No se puede medir sin el nodo, con un degradado
   * lineal (tapa el color de debajo) o con una imagen sin color opaco (no deja saber cuál es).
   * ponytail: no ve el fondo que pinta un pseudo-elemento, una foto que tape un color claro ni la
   * opacity de los contenedores; si hace falta, medir sobre la captura.
   */
  private static final String CONTRASTE_SIN_MEDIR =
      """
      selector => {
        const rgba = valor => valor.startsWith('rgb') ? valor.match(/[\\d.]+/g).map(Number) : null;
        const sobre = (arriba, abajo) =>
          [0, 1, 2].map(i => arriba[i] * (arriba[3] ?? 1) + abajo[i] * (1 - (arriba[3] ?? 1)));
        const luz = color => color.map(v => v / 255)
          .map(v => v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4)
          .reduce((suma, v, i) => suma + v * [0.2126, 0.7152, 0.0722][i], 0);
        const texto = document.querySelector(selector);
        if (!texto) return null;
        const fondos = [];
        for (let caja = texto; caja; caja = caja.parentElement) {
          const estilo = getComputedStyle(caja);
          const fondo = rgba(estilo.backgroundColor);
          const opaco = fondo && (fondo[3] ?? 1) === 1;
          if (!fondo || estilo.backgroundImage.includes('linear-gradient')
              || (estilo.backgroundImage !== 'none' && !opaco)) return null;
          fondos.unshift(fondo);
          if (opaco) break;
        }
        const fondo = fondos.reduce((abajo, arriba) => sobre(arriba, abajo), [255, 255, 255]);
        const estilo = getComputedStyle(texto);
        const color = rgba(estilo.color);
        if (!color) return null;
        const [clara, oscura] = [luz(sobre(color, fondo)), luz(fondo)].sort((a, b) => b - a);
        const razon = (clara + 0.05) / (oscura + 0.05);
        const tamano = parseFloat(estilo.fontSize);
        const minimo = tamano >= 24 || (tamano >= 18.66 && estilo.fontWeight >= 700) ? 3 : 4.5;
        return razon < minimo ? razon.toFixed(2) + ':1, pide ' + minimo + ':1' : null;
      }
      """;

  /** Hay alguna animación o transición corriendo (CSS o scroll): la pantalla "tiene movimiento". */
  private static final String HAY_MOVIMIENTO = "() => document.getAnimations().length > 0";

  /** Cuánto se graba cada pantalla: la coreografía de entrada de las primitivas dura menos. */
  private static final int MS_DE_VIDEO = 3500;

  private static final int ANCHO_DE_VIDEO = 1280;
  private static final int ALTO_DE_VIDEO = 720;
  private static final Path VIDEOS = CAPTURAS.resolve("videos");

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

  private static final Path CSS = Path.of("src/main/resources/static/css");
  private static final Path VARIANTES_CSS = CSS.resolve("variantes");
  private static final Pattern COMENTARIO = Pattern.compile("/\\*(.*?)\\*/", Pattern.DOTALL);
  private static final String SITIO = "sitio";

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

  /** Lo que se captura en cada pasada: el sitio o una variante A/B (vacía: ninguna). */
  private record Opcion(String variante) {

    String nombre() {
      return variante.isEmpty() ? SITIO : variante;
    }

    String consulta() {
      return variante.isEmpty() ? "" : "variante=" + variante;
    }

    String sufijo() {
      return variante.isEmpty() ? "" : "--" + variante;
    }
  }

  private record Captura(
      String ruta, String variante, int ancho, String archivo, Object secciones) {}

  record Falla(String ruta, String variante, int ancho, String tipo, String detalle) {}

  @LocalServerPort private int puerto;

  @Test
  void paginasCumplenElPisoEnTodosLosAnchos() throws IOException {
    List<Opcion> opciones = opciones();
    List<Captura> capturas = new ArrayList<>();
    List<Falla> fallas = new ArrayList<>();
    Files.createDirectories(CAPTURAS);

    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina = navegador.newPage();
      for (Opcion opcion : opciones) {
        String variante = opcion.nombre();
        for (String ruta : paginas()) {
          for (int ancho : ANCHOS) {
            if (abrir(pagina, ruta, opcion, ancho, fallas)) {
              String archivo = nombreDeCaptura(ruta, opcion, String.valueOf(ancho));
              pagina.evaluate(CARGAR_IMAGENES);
              pagina.screenshot(
                  new Page.ScreenshotOptions()
                      .setPath(CAPTURAS.resolve(archivo))
                      .setFullPage(true)
                      .setAnimations(ScreenshotAnimations.DISABLED));
              capturas.add(new Captura(ruta, variante, ancho, archivo, pagina.evaluate(SECCIONES)));
              if (BOCETO) {
                continue;
              }
              revisarAccesibilidad(pagina, ruta, variante, ancho, fallas);
              revisarToque(pagina, ruta, variante, ancho, fallas);
              revisarDesborde(pagina, ruta, variante, ancho, fallas);
              if (ancho == AnchosDeReferencia.CELULAR) {
                capturarPrimeraPantalla(pagina, ruta, opcion);
              }
            }
          }
          for (int ancho : BOCETO ? List.<Integer>of() : AnchosDeReferencia.INTERMEDIOS) {
            if (abrir(pagina, ruta, opcion, ancho, fallas)) {
              revisarDesborde(pagina, ruta, variante, ancho, fallas);
            }
          }
        }
      }
    }

    Files.writeString(
        CAPTURAS.resolve("index.html"), visor(capturas, opciones), StandardCharsets.UTF_8);
    if (opciones.size() > 1) {
      Files.writeString(
          CAPTURAS.resolve("comparacion.html"), visor(capturas, opciones), StandardCharsets.UTF_8);
    }
    JsonMapper.builder()
        .build()
        .writerWithDefaultPrettyPrinter()
        .writeValue(CAPTURAS.resolve("reporte.json"), fallas);

    assertThat(fallas).as("Detalle en %s", CAPTURAS.resolve("reporte.json")).isEmpty();
  }

  /**
   * B0-09 (D32): por cada pantalla de la lista que tiene movimiento al abrirse, un video de {@code
   * MS_DE_VIDEO} ms en {@code target/capturas/videos/<ruta>.webm}; las que no se mueven no dejan
   * video. Con "reducir movimiento" en el navegador, ninguna pantalla puede tener animaciones
   * corriendo (D32: se resuelve dentro de las primitivas).
   */
  @Test
  void lasPantallasConMovimientoDejanSuVideoYRespetanReducirMovimiento() throws IOException {
    Files.createDirectories(VIDEOS);
    List<Falla> fallas = new ArrayList<>();
    List<String> grabadas = new ArrayList<>();
    Opcion sitio = new Opcion("");
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina = navegador.newPage();
      for (String ruta : paginas()) {
        if (!abrir(pagina, ruta, sitio, ANCHO_DE_VIDEO, fallas)
            || !Boolean.TRUE.equals(pagina.evaluate(HAY_MOVIMIENTO))) {
          continue;
        }
        grabadas.add(grabar(navegador, ruta, sitio));
        revisarReducirMovimiento(navegador, ruta, fallas);
      }
    }
    JsonMapper.builder()
        .build()
        .writerWithDefaultPrettyPrinter()
        .writeValue(VIDEOS.resolve("reporte.json"), Map.of("videos", grabadas, "fallas", fallas));
    assertThat(grabadas).as("Al menos /estilos y la portada se mueven").isNotEmpty();
    assertThat(fallas).as("Detalle en %s", VIDEOS.resolve("reporte.json")).isEmpty();
  }

  /**
   * La cola de avisos (B2-04, Alpine): de cuatro avisos en la región se ven tres, al cerrar uno
   * aparece el cuarto, y los que quedan se van solos pasados unos segundos.
   */
  @Test
  void laColaDeAvisosMuestraTresALaVezYLosQuita() {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina = navegador.newPage();
      pagina.navigate("http://localhost:" + puerto + "/estilos");
      pagina.waitForFunction(
          "() => window.Alpine && document.getElementById('avisos')._x_dataStack");
      for (int i = 0; i < 4; i++) {
        pagina.getByText("Mostrar un aviso en la región").click();
      }
      assertThat(pagina.locator("#avisos .aviso-breve").count()).isEqualTo(4);
      assertThat(pagina.locator("#avisos .aviso-breve:not([hidden])").count()).isEqualTo(3);
      pagina.locator("#avisos .aviso-cerrar").first().click();
      pagina.mouse().move(0, 0);
      pagina.waitForFunction(
          "() => document.querySelectorAll('#avisos .aviso-breve').length === 3"
              + " && document.querySelectorAll('#avisos .aviso-breve:not([hidden])').length === 3");
      pagina.waitForFunction(
          "() => document.querySelectorAll('#avisos .aviso-breve').length === 0",
          null,
          new Page.WaitForFunctionOptions().setTimeout(15_000));
    }
  }

  /**
   * El diálogo (B2-05) se abre con su botón sin JavaScript propio, lleva el foco y se cierra con
   * Esc.
   */
  @Test
  void elDialogoSeAbreConSuBotonYSeCierraConEscape() {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina = navegador.newPage();
      pagina.navigate("http://localhost:" + puerto + "/estilos");
      String abierto =
          "() => document.getElementById('dialogo-de-muestra').matches(':popover-open')";
      assertThat(pagina.evaluate(abierto)).isEqualTo(false);
      pagina.locator("[popovertarget='dialogo-de-muestra']:not([popovertargetaction])").click();
      assertThat(pagina.evaluate(abierto)).isEqualTo(true);
      assertThat(
              pagina.evaluate(
                  "() => document.getElementById('dialogo-de-muestra').contains(document.activeElement)"))
          .isEqualTo(true);
      pagina.keyboard().press("Escape");
      assertThat(pagina.evaluate(abierto)).isEqualTo(false);
    }
  }

  /** Abre la pantalla en un contexto con video, espera y deja el .webm con el nombre de la ruta. */
  private String grabar(Browser navegador, String ruta, Opcion opcion) {
    String archivo = nombreDeCaptura(ruta, opcion, "movimiento").replace(".png", ".webm");
    try (BrowserContext contexto =
        navegador.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(ANCHO_DE_VIDEO, ALTO_DE_VIDEO)
                .setRecordVideoDir(VIDEOS)
                .setRecordVideoSize(ANCHO_DE_VIDEO, ALTO_DE_VIDEO))) {
      Page pagina = contexto.newPage();
      pagina.navigate("http://localhost:" + puerto + ruta);
      pagina.waitForTimeout(MS_DE_VIDEO);
      Video video = pagina.video();
      pagina.close();
      video.saveAs(VIDEOS.resolve(archivo));
      video.delete();
    }
    return archivo;
  }

  private void revisarReducirMovimiento(Browser navegador, String ruta, List<Falla> fallas) {
    try (BrowserContext contexto =
        navegador.newContext(
            new Browser.NewContextOptions().setReducedMotion(ReducedMotion.REDUCE))) {
      Page pagina = contexto.newPage();
      pagina.navigate("http://localhost:" + puerto + ruta);
      if (Boolean.TRUE.equals(pagina.evaluate(HAY_MOVIMIENTO))) {
        fallas.add(
            new Falla(
                ruta,
                SITIO,
                ANCHO_DE_VIDEO,
                "movimiento",
                "tiene animaciones con \"reducir movimiento\""));
      }
    }
  }

  private boolean abrir(Page pagina, String ruta, Opcion opcion, int ancho, List<Falla> fallas) {
    pagina.setViewportSize(ancho, AnchosDeReferencia.ALTO);
    String separador = ruta.contains("?") ? "&" : "?";
    String consulta = opcion.consulta();
    String url =
        "http://localhost:" + puerto + ruta + (consulta.isEmpty() ? "" : separador + consulta);
    Response respuesta = pagina.navigate(url);
    Matcher codigo = RESPUESTA_PEDIDA.matcher(ruta);
    boolean esperada =
        codigo.find()
            && respuesta != null
            && respuesta.status() == Integer.parseInt(codigo.group(1));
    if (respuesta == null || (!respuesta.ok() && !esperada)) {
      int estado = respuesta == null ? 0 : respuesta.status();
      fallas.add(
          new Falla(ruta, opcion.nombre(), ancho, "pagina", "HTTP " + estado + " en " + url));
      return false;
    }
    return true;
  }

  private static void revisarAccesibilidad(
      Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    AxeResults resultado = new AxeBuilder(pagina).withTags(NORMAS_AXE).analyze();
    resultado.getIncomplete().stream()
        .filter(regla -> "color-contrast".equals(regla.getId()))
        .flatMap(regla -> regla.getNodes().stream())
        .map(nodo -> ((List<?>) nodo.getTarget()).get(0).toString())
        .forEach(
            selector -> {
              if (pagina.evaluate(CONTRASTE_SIN_MEDIR, selector) instanceof String razon) {
                fallas.add(
                    new Falla(ruta, variante, ancho, "contraste", razon + " en " + selector));
              }
            });
    for (Rule regla : resultado.getViolations()) {
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

  /** Lo que se ve en el celular sin desplazar, aparte de la página completa. */
  private static void capturarPrimeraPantalla(Page pagina, String ruta, Opcion opcion) {
    pagina.setViewportSize(AnchosDeReferencia.CELULAR, AnchosDeReferencia.ALTO_CELULAR);
    pagina.evaluate("() => window.scrollTo(0, 0)");
    String medida = AnchosDeReferencia.CELULAR + "x" + AnchosDeReferencia.ALTO_CELULAR;
    pagina.screenshot(
        new Page.ScreenshotOptions()
            .setPath(CAPTURAS.resolve(nombreDeCaptura(ruta, opcion, medida)))
            .setAnimations(ScreenshotAnimations.DISABLED));
  }

  /**
   * Las rutas de -Dverificacion.paginas=/a,/b o, sin la propiedad, las de paginas.txt. La barra
   * inicial es opcional: Git Bash convierte "=/a" en una ruta de Windows.
   */
  private static List<String> paginas() throws IOException {
    String pedidas = System.getProperty("verificacion.paginas", "").strip();
    Stream<String> lineas =
        pedidas.isEmpty()
            ? Files.readAllLines(PAGINAS, StandardCharsets.UTF_8).stream()
            : Arrays.stream(pedidas.split(","));
    return lineas
        .map(String::strip)
        .filter(linea -> !linea.isEmpty() && !linea.startsWith("#"))
        .map(ruta -> ruta.startsWith("/") ? ruta : "/" + ruta)
        .toList();
  }

  /** Con variantes, una pasada por variante; sin variantes, una pasada por el sitio. */
  private static List<Opcion> opciones() {
    List<String> variantes =
        Arrays.stream(System.getProperty("verificacion.variantes", "").split(","))
            .map(String::strip)
            .filter(v -> !v.isEmpty())
            .toList();
    if (!variantes.isEmpty()) {
      return variantes.stream().map(Opcion::new).toList();
    }
    return List.of(new Opcion(""));
  }

  /** {@code <ruta>[--<variante>]-<medida>.png}; la medida es el ancho o 360x640. */
  private static String nombreDeCaptura(String ruta, Opcion opcion, String medida) {
    String base = ruta.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "");
    return (base.isEmpty() ? "inicio" : base) + opcion.sufijo() + "-" + medida + ".png";
  }

  /**
   * Datos del visor: capturas por página, variante y ancho, con la descripción de cada variante.
   */
  private static String visor(List<Captura> capturas, List<Opcion> opciones) throws IOException {
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
    for (Opcion opcion : opciones) {
      descritas.add(Map.of("nombre", opcion.nombre(), "descripcion", descripcion(opcion)));
    }
    String datos =
        JsonMapper.builder()
            .build()
            .writeValueAsString(
                Map.of(
                    "variantes", descritas,
                    "capturas", porPagina,
                    "anchos", ANCHOS))
            .replace("</", "<\\/");
    return VISOR
        .replace("@TITULO@", HtmlUtils.htmlEscape(titulo(opciones)))
        .replace("@DATOS@", datos);
  }

  private static String titulo(List<Opcion> opciones) {
    List<String> nombres = opciones.stream().map(Opcion::nombre).toList();
    return nombres.size() > 1
        ? "Comparación: " + String.join(" / ", nombres)
        : "Capturas: " + nombres.get(0);
  }

  /** Primer comentario del CSS de la variante, en una línea; vacío sin variante o si no hay. */
  private static String descripcion(Opcion opcion) throws IOException {
    Path css = VARIANTES_CSS.resolve(opcion.variante() + ".css");
    if (opcion.variante().isEmpty() || !Files.exists(css)) {
      return "";
    }
    Matcher comentario = COMENTARIO.matcher(Files.readString(css, StandardCharsets.UTF_8));
    return comentario.find() ? comentario.group(1).replaceAll("\\s*\\*\\s*", " ").strip() : "";
  }
}
