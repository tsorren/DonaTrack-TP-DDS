package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ReducedMotion;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * R11-33 y R11-40 (ficha docs/interfaz/flujos/r11-p2-armazon.md, criterios de la sección 9): lo que
 * queda fijo al desplazarse (la cabecera desde la tableta, la barra de filtros de los listados y el
 * botón principal de Confirmar recepción en el celular) y la pantalla de carga, que se va sola sin
 * JavaScript. Perfil Maven {@code visual} (necesita Playwright): {@code mvn test -pl
 * cliente-liviano -Pvisual -Dtest=ArmazonFijoTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class ArmazonFijoTest {

  private static final List<String> PANTALLAS =
      List.of(
          "/publico-portada",
          "/donante-inicio",
          "/donante-entidades",
          "/admin-donantes",
          "/entidad-seguimiento-confirmar");

  private static final String CARGA_VISIBLE =
      "() => getComputedStyle(document.body, '::before').visibility === 'visible'";

  /** La cabecera, la barra y la distancia entre las dos, medidas en la ventana. */
  private static final String CABECERA_Y_BARRA =
      """
      () => {
        const c = document.querySelector('body > header').getBoundingClientRect();
        const b = document.querySelector('.filtros-barra').getBoundingClientRect();
        return [Math.round(c.top), Math.round(c.bottom), Math.round(b.top)];
      }
      """;

  @LocalServerPort private int puerto;

  @Test
  void sinJavaScriptLaPantallaDeCargaSeVaSolaAntesDe600Ms() {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch();
        BrowserContext contexto =
            navegador.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false))) {
      Page pagina = contexto.newPage();
      List<String> fallas = new ArrayList<>();
      for (String ruta : PANTALLAS) {
        pagina.navigate(url(ruta));
        pagina.waitForTimeout(600);
        if (Boolean.TRUE.equals(pagina.evaluate(CARGA_VISIBLE))) {
          fallas.add(ruta);
        }
      }
      assertThat(fallas).as("La pantalla de carga sigue a los 0,6 s").isEmpty();
    }
  }

  @Test
  void laPantallaDeCargaSeVeAlEmpezarYNoTapaLosToques() {
    conPagina(
        1280,
        900,
        false,
        pagina -> {
          pagina.navigate(url("/donante-inicio"));
          assertThat(
                  pagina.evaluate(
                      "() => document.getAnimations().some(a => a.animationName"
                          + " === 'pantalla-de-carga')"))
              .isEqualTo(true);
          assertThat(
                  pagina.evaluate(
                      "() => getComputedStyle(document.body, '::before').pointerEvents"))
              .isEqualTo("none");
          pagina.waitForFunction("() => !(" + CARGA_VISIBLE + ")()");
        });
  }

  @Test
  void conReducirMovimientoNoHayPantallaDeCarga() {
    conPagina(
        1280,
        900,
        true,
        pagina -> {
          pagina.navigate(url("/donante-inicio"));
          assertThat(pagina.evaluate(CARGA_VISIBLE)).isEqualTo(false);
          assertThat(pagina.evaluate("() => document.getAnimations().length")).isEqualTo(0);
        });
  }

  @Test
  void laCabeceraYLaBarraQuedanFijasSinHuecoEntreEllas() {
    List<String> fallas = new ArrayList<>();
    for (int ancho : List.of(768, 1280, 1920)) {
      for (String ruta : List.of("/admin-donantes", "/donante-entidades")) {
        conPagina(
            ancho,
            900,
            false,
            pagina -> {
              pagina.navigate(url(ruta));
              pagina.evaluate("() => window.scrollTo(0, 900)");
              List<?> medidas = (List<?>) pagina.evaluate(CABECERA_Y_BARRA);
              int arriba = ((Number) medidas.get(0)).intValue();
              int finCabecera = ((Number) medidas.get(1)).intValue();
              int barra = ((Number) medidas.get(2)).intValue();
              // El papel de la barra sube 6 px: un hueco menor no deja ver la lista.
              if (arriba != 0 || barra < finCabecera - 1 || barra > finCabecera + 6) {
                fallas.add(ruta + " a " + ancho + " px: " + medidas);
              }
            });
      }
    }
    assertThat(fallas)
        .as("[arriba de la cabecera, fin de la cabecera, arriba de la barra]")
        .isEmpty();
  }

  @Test
  void enElCelularArribaNoQuedaNadaFijoYLaBarraSoloEnVentanasAltas() {
    conPagina(
        360,
        640,
        false,
        pagina -> {
          pagina.navigate(url("/donante-entidades"));
          assertThat(posicion(pagina, "body > header")).isNotEqualTo("sticky");
          assertThat(posicion(pagina, ".filtros-barra")).isNotEqualTo("sticky");
        });
    conPagina(
        390,
        844,
        false,
        pagina -> {
          pagina.navigate(url("/donante-entidades"));
          assertThat(posicion(pagina, ".filtros-barra")).isEqualTo("sticky");
          double alto =
              ((Number)
                      pagina.evaluate(
                          "() => document.querySelector('.filtros-barra')"
                              + ".getBoundingClientRect().height"))
                  .doubleValue();
          assertThat(alto).as("La barra fija ocupa hasta un cuarto del alto").isLessThan(844 / 4.0);
        });
  }

  /**
   * Ficha §4.2 y criterio 4 (revisión de p2-armazon, ronda 1): en un celular de 390 × 844, una
   * barra fija entra en un cuarto del alto y en --medida-filtros-fijos; la de la administración,
   * con cinco pastillas, no queda fija.
   */
  @Test
  void enElCelularSoloQuedaFijaLaBarraQueEntraEnUnCuartoDelAlto() {
    List<String> fallas = new ArrayList<>();
    for (String ruta :
        List.of("/admin-donantes", "/admin-donaciones", "/donante-entidades", "/admin-camiones")) {
      conPagina(
          390,
          844,
          false,
          pagina -> {
            pagina.navigate(url(ruta));
            List<?> medidas =
                (List<?>)
                    pagina.evaluate(
                        """
                        () => {
                          const b = document.querySelector('.filtros-barra');
                          const prueba = document.createElement('div');
                          prueba.style.height = 'var(--medida-filtros-fijos)';
                          document.body.append(prueba);
                          const medida = prueba.getBoundingClientRect().height;
                          prueba.remove();
                          return [getComputedStyle(b).position === 'sticky',
                            b.getBoundingClientRect().height, medida];
                        }
                        """);
            boolean fija = Boolean.TRUE.equals(medidas.get(0));
            double alto = ((Number) medidas.get(1)).doubleValue();
            double medida = ((Number) medidas.get(2)).doubleValue();
            if (fija && (alto >= 844 / 4.0 || alto > medida + 0.5)) {
              fallas.add(ruta + ": mide " + alto + " px, la medida es " + medida + " px");
            }
          });
    }
    assertThat(fallas).as("Barras fijas que pasan un cuarto del alto o su medida").isEmpty();
    conPagina(
        390,
        844,
        false,
        pagina -> {
          pagina.navigate(url("/admin-donantes"));
          assertThat(posicion(pagina, ".filtros-barra")).isNotEqualTo("sticky");
        });
  }

  @Test
  void enElCelularConTabYShiftTabElFocoNoQuedaTapado() {
    List<String> tapados = new ArrayList<>();
    for (String ruta : List.of("/admin-donantes", "/donante-entidades")) {
      conPagina(
          390,
          844,
          false,
          pagina -> {
            pagina.navigate(url(ruta));
            pagina.locator("form.filtros-barra .filtro-buscar-fila button").focus();
            for (int i = 0; i < 30; i++) {
              pagina.keyboard().press("Tab");
              anotarSiEstaTapado(pagina, ruta + " (Tab)", tapados);
            }
            for (int i = 0; i < 30; i++) {
              pagina.keyboard().press("Shift+Tab");
              anotarSiEstaTapado(pagina, ruta + " (Shift+Tab)", tapados);
            }
          });
    }
    assertThat(tapados).isEmpty();
  }

  private static void anotarSiEstaTapado(Page pagina, String donde, List<String> tapados) {
    Object tapado =
        pagina.evaluate(
            """
            () => {
              const f = document.activeElement;
              if (!f || !f.closest('main') || f.closest('.filtros-barra')) return null;
              const r = f.getBoundingClientRect();
              const b = document.querySelector('.filtros-barra');
              const fija = getComputedStyle(b).position === 'sticky';
              const despues = b.compareDocumentPosition(f) & Node.DOCUMENT_POSITION_FOLLOWING;
              const tope = fija && despues ? b.getBoundingClientRect().bottom : 0;
              const menu = document.querySelector('nav.navegacion');
              const piso = menu ? menu.getBoundingClientRect().top : innerHeight;
              return r.top < tope - 1 || r.bottom > piso + 1
                ? (f.textContent || f.name || f.tagName).trim().slice(0, 30) : null;
            }
            """);
    if (tapado instanceof String nombre) {
      tapados.add(donde + ": " + nombre);
    }
  }

  @Test
  void enElCelularElBotonDeConfirmarQuedaEncimaDelMenuHastaElFinal() {
    conPagina(
        360,
        640,
        false,
        pagina -> {
          pagina.navigate(url("/entidad-seguimiento-confirmar"));
          String medir =
              """
              () => {
                const b = document.querySelector('[data-fijo="celular"]').getBoundingClientRect();
                const m = document.querySelector('nav.navegacion').getBoundingClientRect();
                return [Math.round(b.bottom), Math.round(m.top)];
              }
              """;
          // Con el principio del formulario a la vista, el botón ya está abajo, encima del menú.
          pagina.evaluate("() => document.querySelector('form.acta-formulario').scrollIntoView()");
          List<?> arriba = (List<?>) pagina.evaluate(medir);
          assertThat(arriba.get(0)).as("pegado encima del menú").isEqualTo(arriba.get(1));
          pagina.evaluate("() => window.scrollTo(0, document.body.scrollHeight)");
          List<?> abajo = (List<?>) pagina.evaluate(medir);
          assertThat(((Number) abajo.get(0)).intValue())
              .as("al final vuelve a su lugar, por encima del menú")
              .isLessThan(((Number) abajo.get(1)).intValue());
        });
    conPagina(
        1280,
        900,
        false,
        pagina -> {
          pagina.navigate(url("/entidad-seguimiento-confirmar"));
          assertThat(
                  pagina.evaluate(
                      "() => getComputedStyle(document.querySelector('[data-fijo]')).display"))
              .isEqualTo("contents");
        });
  }

  @Test
  void conTabElFocoNoQuedaDebajoDeLaCabeceraNiDeLaBarra() {
    conPagina(
        1280,
        900,
        false,
        pagina -> {
          pagina.navigate(url("/admin-donantes"));
          pagina.locator("form.filtros-barra .filtro-buscar-fila button").focus();
          List<String> tapados = new ArrayList<>();
          for (int i = 0; i < 40; i++) {
            pagina.keyboard().press("Tab");
            Object tapado =
                pagina.evaluate(
                    """
                    () => {
                      const f = document.activeElement;
                      if (!f || !f.closest('main') || f.closest('.filtros-barra')) return null;
                      const r = f.getBoundingClientRect();
                      const b = document.querySelector('.filtros-barra');
                      const menu = document.querySelector('body > header').getBoundingClientRect();
                      // La barra solo tapa lo que viene después de ella.
                      const despues = b.compareDocumentPosition(f) & Node.DOCUMENT_POSITION_FOLLOWING;
                      const tope = despues ? Math.max(menu.bottom, b.getBoundingClientRect().bottom)
                        : menu.bottom;
                      return r.top < tope - 1 || r.bottom > innerHeight + 1
                        ? (f.textContent || f.name || f.tagName).trim().slice(0, 30) : null;
                    }
                    """);
            if (tapado instanceof String nombre) {
              tapados.add(nombre);
            }
          }
          assertThat(tapados).isEmpty();
        });
  }

  @Test
  void laPortadaYLasPaginasCarganLasDosLetrasAntes() {
    conPagina(
        1280,
        900,
        false,
        pagina -> {
          for (String ruta :
              List.of("/publico-portada", "/donante-inicio", "/publico-donaciones")) {
            pagina.navigate(url(ruta));
            assertThat(pagina.locator("head link[rel=preload][as=font][crossorigin]").count())
                .as("precargas en %s", ruta)
                .isEqualTo(2);
          }
        });
  }

  /**
   * Criterio 2 de la ficha: con las letras demoradas (llegan después de la pantalla de carga), la
   * página casi no salta al llegar la definitiva gracias a las letras de respaldo ajustadas.
   */
  @Test
  void conLasLetrasDemoradasLaPaginaCasiNoSalta() {
    List<String> fallas = new ArrayList<>();
    for (String ruta : List.of("/publico-portada", "/donante-inicio")) {
      conPagina(
          1280,
          900,
          false,
          pagina -> {
            pagina.route(
                "**/*.woff2",
                pedido -> {
                  pagina.waitForTimeout(900);
                  pedido.resume();
                });
            pagina.addInitScript(
                "window.__salto = 0; new PerformanceObserver(l => l.getEntries().forEach(e => {"
                    + " if (!e.hadRecentInput) window.__salto += e.value; }))"
                    + ".observe({type: 'layout-shift', buffered: true});");
            pagina.navigate(url(ruta));
            pagina.evaluate("() => document.fonts.ready");
            pagina.waitForTimeout(300);
            double salto = ((Number) pagina.evaluate("() => window.__salto")).doubleValue();
            if (salto >= 0.05) {
              fallas.add(ruta + ": " + salto);
            }
          });
    }
    assertThat(fallas).as("Desplazamiento acumulado de diseño menor a 0,05").isEmpty();
  }

  private static String posicion(Page pagina, String selector) {
    return (String)
        pagina.evaluate("s => getComputedStyle(document.querySelector(s)).position", selector);
  }

  private String url(String ruta) {
    return "http://localhost:" + puerto + ruta;
  }

  private static void conPagina(int ancho, int alto, boolean reducir, Consumer<Page> prueba) {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch();
        BrowserContext contexto =
            navegador.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(ancho, alto)
                    .setReducedMotion(
                        reducir ? ReducedMotion.REDUCE : ReducedMotion.NO_PREFERENCE))) {
      prueba.accept(contexto.newPage());
    }
  }
}
