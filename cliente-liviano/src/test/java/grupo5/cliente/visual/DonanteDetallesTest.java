package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Los detalles del donante (docs/interfaz/backlog-revision-2026-11-11.md, R11-37 y R11-39): "Ver mi
 * huerta" queda al medio de su lámina; en Misiones, tocar afuera o Esc suelta la maceta elegida; y
 * lo que espera destino en Seguir entregas es un aviso con ícono, título y lista. Corre con {@code
 * -Pvisual}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class DonanteDetallesTest {

  private static final List<Integer> ANCHOS = List.of(360, 768, 1024, 1280, 1920);
  private static final String MACETA = "/donante-misiones?cuenta=persona&lugar=estante-izq";
  private static final String BOTON = "#anio-huerta-dibujo .boton";

  /** Lo que sobra a la izquierda y a la derecha del contenido de la lámina (título y dibujo). */
  private static final String HOLGURAS =
      """
      () => {
        const caja = document.querySelector('#anio-huerta').getBoundingClientRect();
        const partes = ['#anio-huerta .huerta-cabeza', '#anio-huerta-dibujo']
          .map(s => document.querySelector(s).getBoundingClientRect());
        const izquierda = Math.min(...partes.map(p => p.left));
        const derecha = Math.max(...partes.map(p => p.right));
        return [izquierda - caja.left, caja.right - derecha];
      }
      """;

  /** El botón entero dentro del dibujo. */
  private static final String BOTON_EN_EL_DIBUJO =
      """
      selector => {
        const d = document.querySelector('#anio-huerta-dibujo').getBoundingClientRect();
        const b = document.querySelector(selector).getBoundingClientRect();
        return b.left >= d.left && b.right <= d.right;
      }
      """;

  private static final String SIN_DESPLAZAR_DE_COSTADO =
      "() => document.documentElement.scrollWidth <= document.documentElement.clientWidth";

  @LocalServerPort private int puerto;

  @Test
  void verMiHuertaQuedaCentradaEnTodosLosAnchos() {
    for (int ancho : ANCHOS) {
      conPagina(
          "/donante-inicio?cuenta=persona",
          true,
          ancho,
          pagina -> {
            List<?> holguras = (List<?>) pagina.evaluate(HOLGURAS);
            double izquierda = ((Number) holguras.get(0)).doubleValue();
            double derecha = ((Number) holguras.get(1)).doubleValue();
            assertThat(izquierda)
                .as("a %d px, lo que sobra a izquierda y derecha", ancho)
                .isCloseTo(derecha, within(2.0));
            if (ancho >= 1280) {
              assertThat(izquierda)
                  .as(
                      "a %d px, el título y el dibujo van juntos al medio, no uno en cada punta",
                      ancho)
                  .isGreaterThan(48);
            }
            assertThat(
                    pagina.evaluate(
                        "() => getComputedStyle(document.querySelector('#anio-huerta h2')).textAlign"))
                .as("el título va al medio de su columna")
                .isEqualTo("center");
            assertThat(pagina.locator(BOTON).isVisible()).isTrue();
            assertThat(pagina.evaluate(BOTON_EN_EL_DIBUJO, BOTON))
                .as("el botón queda dentro del dibujo")
                .isEqualTo(true);
          });
    }
  }

  @Test
  void tocarAfueraSueltaLaMacetaElegida() {
    conPagina(
        MACETA,
        true,
        1280,
        pagina -> {
          assertThat(elegida(pagina)).isTrue();
          pagina.locator("#contenido h1").first().click();
          pagina.waitForURL(url -> !url.contains("lugar="));
          assertThat(elegida(pagina)).isFalse();
        });
  }

  @Test
  void escSueltaLaMacetaElegida() {
    conPagina(
        MACETA,
        true,
        1280,
        pagina -> {
          assertThat(elegida(pagina)).isTrue();
          pagina.keyboard().press("Escape");
          pagina.waitForURL(url -> !url.contains("lugar="));
          assertThat(elegida(pagina)).isFalse();
        });
  }

  @Test
  void tocarLaFichaOOtraMacetaNoSueltaNada() {
    conPagina(
        MACETA,
        true,
        1280,
        pagina -> {
          pagina.locator("#huerta-ficha h3").first().click();
          pagina.waitForTimeout(400);
          assertThat(pagina.url()).contains("lugar=estante-izq");
          assertThat(elegida(pagina)).isTrue();

          pagina.locator("#huerta-lugares li[data-lugar='mesada-medio'] a").click();
          pagina.waitForURL(url -> url.contains("lugar=mesada-medio"));
          assertThat(elegida(pagina)).isTrue();
        });
  }

  @Test
  void conUnMenuAbiertoEscSoloLoCierra() {
    conPagina(
        MACETA,
        true,
        1280,
        pagina -> {
          pagina.locator("details.navegacion-cuenta > summary").click();
          pagina.keyboard().press("Escape");
          pagina.waitForTimeout(400);
          assertThat(pagina.locator("details.navegacion-cuenta[open]").count()).isZero();
          assertThat(pagina.url()).contains("lugar=estante-izq");
        });
  }

  @Test
  void sinJavaScriptSigueElEnlaceDeVerTodaLaHuerta() {
    conPagina(
        MACETA,
        false,
        1280,
        pagina -> {
          pagina.locator("a[data-suelta]").click();
          pagina.waitForURL(url -> !url.contains("lugar="));
          assertThat(elegida(pagina)).isFalse();
        });
  }

  @Test
  void loQueEsperaDestinoEsUnAvisoConIconoTituloYLista() {
    for (int ancho : List.of(360, 1280)) {
      conPagina(
          "/donante-seguimiento?cuenta=persona",
          true,
          ancho,
          pagina -> {
            String aviso = "[data-v='sin-destino']";
            assertThat(pagina.locator(aviso).isVisible()).isTrue();
            assertThat(pagina.locator(aviso + " > svg").isVisible()).isTrue();
            assertThat(pagina.locator(aviso + " h3").innerText()).startsWith("Todavía espera");
            assertThat(pagina.locator(aviso + " li").count()).isPositive();
            assertThat(pagina.evaluate(SIN_DESPLAZAR_DE_COSTADO))
                .as("sin desplazamiento horizontal a %d px", ancho)
                .isEqualTo(true);
          });
    }
  }

  private void conPagina(String ruta, boolean conJavaScript, int ancho, Consumer<Page> prueba) {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch();
        BrowserContext contexto =
            navegador.newContext(
                new Browser.NewContextOptions()
                    .setJavaScriptEnabled(conJavaScript)
                    .setViewportSize(ancho, 900))) {
      Page pagina = contexto.newPage();
      pagina.navigate("http://localhost:" + puerto + ruta);
      pagina.waitForLoadState();
      prueba.accept(pagina);
    }
  }

  private static boolean elegida(Page pagina) {
    return pagina.locator("#huerta[data-elegido]").count() > 0;
  }
}
