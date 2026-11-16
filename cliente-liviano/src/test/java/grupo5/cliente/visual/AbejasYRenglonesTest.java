package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ReducedMotion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Los detalles de la portada (docs/interfaz/backlog-revision-2026-11-11.md, R11-36 y R11-38): las
 * abejas del hero entran y salen de detrás de las macetas con fundido (nunca cambian de capa
 * estando a la vista); con "reducir movimiento" quedan quietas delante; y "lo que donás" y "echa
 * raíces" van enteros en su renglón. Corre con {@code -Pvisual}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class AbejasYRenglonesTest {

  /** Lo más opaca que puede estar una abeja en el cuadro en que cambia de capa. */
  private static final double OPACIDAD_AL_CAMBIAR = 0.15;

  private static final int ANCHO_ESCRITORIO = AnchosDeReferencia.REFERENCIA.get(1);

  /** Anota, en cada cuadro, la capa y la opacidad de cada abeja del hero. */
  private static final String ANOTAR =
      """
      () => {
        window.__cuadros = [];
        const anotar = () => {
          const abejas = [...document.querySelectorAll('.huerta-laminas .huerta-abeja')];
          window.__cuadros.push(abejas.map(a => ({
            capa: a.dataset.capa || '',
            opacidad: a.style.opacity === '' ? 1 : Number(a.style.opacity),
          })));
          requestAnimationFrame(anotar);
        };
        requestAnimationFrame(anotar);
      }
      """;

  /** La capa, la opacidad y los estilos en línea de cada abeja del hero, ya calculados. */
  private static final String ESTADO_DE_LAS_ABEJAS =
      """
      () => [...document.querySelectorAll('.huerta-laminas .huerta-abeja')].map(a =>
        [getComputedStyle(a).zIndex, getComputedStyle(a).opacity, a.dataset.capa || '',
         a.getAttribute('style') || ''].join('|'))
      """;

  /** Cuántos renglones ocupa un texto dentro de un elemento (la primera aparición). */
  private static final String RENGLONES_DEL_TEXTO =
      """
      ([selector, texto]) => {
        const raiz = document.querySelector(selector);
        const recorrido = document.createTreeWalker(raiz, NodeFilter.SHOW_TEXT);
        let nodo;
        while ((nodo = recorrido.nextNode())) {
          const normal = nodo.textContent.replace(/\\u00a0/g, ' ');
          const desde = normal.indexOf(texto);
          if (desde >= 0) {
            const rango = document.createRange();
            rango.setStart(nodo, desde);
            rango.setEnd(nodo, desde + texto.length);
            const cajas = [...rango.getClientRects()].filter(r => r.width > 0);
            /* Un renglón nuevo empieza cuando el centro de una caja cae fuera de la altura del
               primero: los glifos de otra letra de respaldo cambian el alto, no el renglón. */
            const centros = cajas.map(r => (r.top + r.bottom) / 2);
            const alto = Math.min(...cajas.map(r => r.height));
            const renglones = [];
            centros.sort((a, b) => a - b).forEach(c => {
              if (!renglones.some(r => Math.abs(r - c) < alto / 2)) renglones.push(c);
            });
            return renglones.length;
          }
        }
        return -1;
      }
      """;

  @LocalServerPort private int puerto;

  @Test
  void lasAbejasCambianDeCapaSoloCuandoNoSeVen() {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina =
          navegador.newPage(new Browser.NewPageOptions().setViewportSize(ANCHO_ESCRITORIO, 900));
      pagina.clock().install();
      pagina.addInitScript("(" + ANOTAR + ")()");
      pagina.navigate("http://localhost:" + puerto + "/publico-portada");
      /* Un ciclo entero de la ruta más larga dura unos 50 s: se adelanta el reloj de la página. */
      pagina.clock().runFor(60_000);
      List<?> cuadros = (List<?>) pagina.evaluate("() => window.__cuadros");
      assertThat(cuadros).as("cuadros anotados").hasSizeGreaterThan(1000);

      List<String> saltos = new ArrayList<>();
      boolean huboCambio = false;
      boolean huboFundido = false;
      int abejas = ((List<?>) cuadros.get(0)).size();
      assertThat(abejas).isEqualTo(3);
      for (int i = 0; i < abejas; i++) {
        String capaAntes = null;
        double opacidadAntes = 1;
        for (int c = 0; c < cuadros.size(); c++) {
          Map<?, ?> dato = (Map<?, ?>) ((List<?>) cuadros.get(c)).get(i);
          String capa = (String) dato.get("capa");
          double opacidad = ((Number) dato.get("opacidad")).doubleValue();
          /* Sin capa, la abeja aún no empezó a volar: se la ubica por primera vez, no cambia. */
          if (capa.isEmpty()) {
            continue;
          }
          if (capaAntes != null && !capa.equals(capaAntes)) {
            huboCambio = true;
            if (opacidad > OPACIDAD_AL_CAMBIAR || opacidadAntes > OPACIDAD_AL_CAMBIAR) {
              saltos.add("abeja " + i + ", cuadro " + c + ": " + opacidadAntes + " a " + opacidad);
            }
          }
          huboFundido |= opacidad < 1;
          capaAntes = capa;
          opacidadAntes = opacidad;
        }
      }
      assertThat(huboCambio).as("alguna abeja pasó por detrás de una maceta").isTrue();
      assertThat(huboFundido).as("hay fundido").isTrue();
      assertThat(saltos).as("cambios de capa a la vista").isEmpty();
      pagina.close();
    }
  }

  @Test
  void conReducirMovimientoLasAbejasQuedanQuietasDelanteYEnteras() {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina =
          navegador.newPage(
              new Browser.NewPageOptions()
                  .setViewportSize(ANCHO_ESCRITORIO, 900)
                  .setReducedMotion(ReducedMotion.REDUCE));
      pagina.clock().install();
      pagina.navigate("http://localhost:" + puerto + "/publico-portada");
      pagina.clock().runFor(5_000);
      List<Object> abejas = new ArrayList<>((List<?>) pagina.evaluate(ESTADO_DE_LAS_ABEJAS));
      /* z-index 4 (delante de las macetas), opacidad 1, sin capa y sin estilos puestos a mano. */
      assertThat(abejas).containsExactly("4|1||", "4|1||", "4|1||");
      pagina.close();
    }
  }

  @Test
  void loQueDonasYEchaRaicesVanEnterosEnSuRenglonEnTodosLosAnchos() {
    List<String> fallas = new ArrayList<>();
    List<Integer> anchos = new ArrayList<>(AnchosDeReferencia.REFERENCIA);
    anchos.addAll(AnchosDeReferencia.INTERMEDIOS);
    anchos.add(320);
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      for (int ancho : anchos) {
        Page pagina = navegador.newPage(new Browser.NewPageOptions().setViewportSize(ancho, 900));
        pagina.navigate("http://localhost:" + puerto + "/publico-portada");
        pagina.evaluate("() => document.fonts.ready");
        medir(pagina, ancho, ".huerta-cierre h2 em", "lo que donás.", fallas);
        medir(pagina, ancho, ".raices-pie > p", "Lo que donás", fallas);
        medir(pagina, ancho, ".raices-pie > p em", "echa raíces.", fallas);
        if (Boolean.TRUE.equals(
            pagina.evaluate(
                "() => document.documentElement.scrollWidth > document.documentElement.clientWidth"))) {
          fallas.add(ancho + " px: la página se desborda a lo ancho");
        }
        pagina.close();
      }
    }
    assertThat(fallas).isEmpty();
  }

  private void medir(Page pagina, int ancho, String selector, String texto, List<String> fallas) {
    Object renglones = pagina.evaluate(RENGLONES_DEL_TEXTO, List.of(selector, texto));
    if (((Number) renglones).intValue() != 1) {
      fallas.add(ancho + " px: \"" + texto + "\" ocupa " + renglones + " renglones");
    }
  }
}
